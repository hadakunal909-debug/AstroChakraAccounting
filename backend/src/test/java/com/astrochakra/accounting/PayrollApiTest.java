package com.astrochakra.accounting;

import static org.hamcrest.Matchers.hasItem;
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
import com.astrochakra.accounting.repository.BalanceRepository;
import com.astrochakra.accounting.repository.ResourceRepository;
import com.astrochakra.accounting.repository.SalaryPaymentRepository;
import com.astrochakra.accounting.repository.TransactionRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@WithMockUser(username = "tester", roles = {"ADMIN"})
class PayrollApiTest {

    @Autowired MockMvc mvc;
    @Autowired BalanceRepository balances;
    @Autowired ResourceRepository resources;
    @Autowired SalaryPaymentRepository payments;
    @Autowired TransactionRepository txs;

    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    @BeforeEach
    void reset() {
        txs.deleteAll();
        payments.deleteAll();
        resources.deleteAll();
        balances.deleteAll();
        Balance b = new Balance();
        b.setId(UUID.randomUUID());
        b.setBalance(new BigDecimal("10000"));
        b.setLiquidReserve(BigDecimal.ZERO);
        balances.save(b);
    }

    @Test
    void resource_create_and_list() throws Exception {
        mvc.perform(post("/api/resources").contentType(JSON)
                .content("{\"name\":\"Ravi\",\"role\":\"Animator\",\"pay_type\":\"fixed\",\"pay_amount\":5000,\"job_label\":\"per month\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.name", is("Ravi")))
           .andExpect(jsonPath("$.pay_type", is("fixed")));
        mvc.perform(get("/api/resources"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[*].name", hasItem("Ravi")));
    }

    @Test
    void salaryPayment_reducesBalance_logsPayment_andWritesTransaction() throws Exception {
        mvc.perform(post("/api/salary-payments").contentType(JSON)
                .content("{\"resource_name\":\"Ravi\",\"pay_type\":\"fixed\",\"amount\":5000,\"fund_source\":\"available\",\"period\":\"2026-04\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.resource_name", is("Ravi")));

        // Bank balance dropped by the salary amount
        mvc.perform(get("/api/balance")).andExpect(content().json("{\"balance\":5000}"));
        // Payment logged
        mvc.perform(get("/api/salary-payments")).andExpect(jsonPath("$[0].resource_name", is("Ravi")));
        // And a Salary expense transaction was written
        mvc.perform(get("/api/transactions")).andExpect(jsonPath("$[*].category", hasItem("Salary")));
    }
}
