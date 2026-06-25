package com.astrochakra.accounting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.astrochakra.accounting.domain.Balance;
import com.astrochakra.accounting.domain.Project;
import com.astrochakra.accounting.repository.BalanceRepository;
import com.astrochakra.accounting.repository.ProjectRepository;
import com.astrochakra.accounting.repository.TransactionRepository;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@WithMockUser(username = "tester", roles = {"ADMIN"})
class MoneyApiTest {

    @Autowired MockMvc mvc;
    @Autowired BalanceRepository balances;
    @Autowired ProjectRepository projects;
    @Autowired TransactionRepository txs;

    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    @BeforeEach
    void reset() {
        txs.deleteAll();
        balances.deleteAll();
        projects.deleteAll();
        Balance b = new Balance();
        b.setId(UUID.randomUUID());
        b.setBalance(new BigDecimal("10000"));
        b.setLiquidReserve(new BigDecimal("2000"));
        balances.save(b);
        Project p = new Project();
        p.setId(UUID.randomUUID());
        p.setCode("PRJ-X");
        p.setName("Project X");
        p.setFixedBudget(new BigDecimal("5000"));
        p.setAllocated(new BigDecimal("5000"));
        p.setColor("#ffffff");
        projects.save(p);
    }

    @Test
    void income_increasesBalance() throws Exception {
        mvc.perform(post("/api/money/transaction").contentType(JSON)
                .content("{\"date\":\"2026-04-15\",\"person\":\"A\",\"amount\":500,\"kind\":\"income\",\"fund_source\":\"available\"}"))
           .andExpect(status().isOk());
        mvc.perform(get("/api/balance")).andExpect(content().json("{\"balance\":10500,\"liquid_reserve\":2000}"));
    }

    @Test
    void availableExpense_reducesBalanceOnly() throws Exception {
        mvc.perform(post("/api/money/transaction").contentType(JSON)
                .content("{\"date\":\"2026-04-15\",\"person\":\"A\",\"amount\":300,\"kind\":\"general_expense\",\"fund_source\":\"available\"}"))
           .andExpect(status().isOk());
        mvc.perform(get("/api/balance")).andExpect(content().json("{\"balance\":9700,\"liquid_reserve\":2000}"));
    }

    @Test
    void reserveExpense_reducesBalanceAndReserve() throws Exception {
        mvc.perform(post("/api/money/transaction").contentType(JSON)
                .content("{\"date\":\"2026-04-15\",\"person\":\"A\",\"amount\":200,\"kind\":\"general_expense\",\"fund_source\":\"reserve\"}"))
           .andExpect(status().isOk());
        mvc.perform(get("/api/balance")).andExpect(content().json("{\"balance\":9800,\"liquid_reserve\":1800}"));
    }

    @Test
    void projectFundExpense_reducesBalanceAndProjectAllocation() throws Exception {
        mvc.perform(post("/api/money/transaction").contentType(JSON)
                .content("{\"date\":\"2026-04-15\",\"person\":\"A\",\"amount\":1000,\"kind\":\"project_expense\",\"project_code\":\"PRJ-X\",\"fund_source\":\"PRJ-X\"}"))
           .andExpect(status().isOk());
        mvc.perform(get("/api/balance")).andExpect(content().json("{\"balance\":9000}"));
        String projectsJson = mvc.perform(get("/api/projects")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        BigDecimal allocated = new BigDecimal(JsonPath.read(projectsJson, "$[0].allocated").toString());
        assertThat(allocated).isEqualByComparingTo("4000");
    }

    @Test
    void settle_restoresBalance_andWritesJv() throws Exception {
        String body = mvc.perform(post("/api/money/transaction").contentType(JSON)
                .content("{\"date\":\"2026-04-15\",\"person\":\"A\",\"amount\":400,\"kind\":\"general_expense\",\"fund_source\":\"available\"}"))
           .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");
        mvc.perform(get("/api/balance")).andExpect(content().json("{\"balance\":9600}"));

        mvc.perform(post("/api/money/transaction/" + id + "/settle").contentType(JSON).content("{\"note\":\"correction\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.is_reversal", is(true)))
           .andExpect(jsonPath("$.kind", is("jv_reversal")));
        mvc.perform(get("/api/balance")).andExpect(content().json("{\"balance\":10000}"));
    }

    @Test
    void reconcile_setsBalance_zeroesReserve_andReports() throws Exception {
        mvc.perform(post("/api/money/reconcile").contentType(JSON).content("{\"true_balance\":12345}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.new_balance", is(12345)));
        mvc.perform(get("/api/balance")).andExpect(content().json("{\"balance\":12345,\"liquid_reserve\":0}"));
    }

    @Test
    void import_insertsRows_andSetsClosingBalance() throws Exception {
        mvc.perform(post("/api/money/import").contentType(JSON).content(
                "{\"closing_balance\":99999,\"rows\":[" +
                "{\"date\":\"2026-04-10\",\"person\":\"Bank\",\"amount\":1000,\"kind\":\"income\"}," +
                "{\"date\":\"2026-04-11\",\"person\":\"Bank\",\"amount\":400,\"kind\":\"general_expense\"}]}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.imported", is(2)));
        mvc.perform(get("/api/balance")).andExpect(content().json("{\"balance\":99999}"));
    }
}
