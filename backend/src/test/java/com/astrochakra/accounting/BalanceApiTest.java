package com.astrochakra.accounting;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.astrochakra.accounting.repository.BalanceRepository;

/**
 * In-process verification of the Phase 0 vertical slice (controller -> service -> JPA -> H2).
 * Uses MockMvc so no real Tomcat/network socket is needed.
 * @WithMockUser satisfies the authenticated() rule added in Phase 1.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@WithMockUser(username = "tester", roles = {"ADMIN"})
class BalanceApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    BalanceRepository balances;

    @BeforeEach
    void reset() {
        // Start from an empty balance table so the "defaults to zero" check is order-independent.
        balances.deleteAll();
    }

    @Test
    void health_isOk() throws Exception {
        mvc.perform(get("/api/health"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status", is("ok")));
    }

    @Test
    void balance_defaults_then_partialUpdates_andUsesSnakeCaseJson() throws Exception {
        // Starts empty -> zeroed default, and the JSON key is snake_case (liquid_reserve)
        mvc.perform(get("/api/balance"))
           .andExpect(status().isOk())
           .andExpect(content().json("{\"balance\":0,\"liquid_reserve\":0}"));

        // Set only balance
        mvc.perform(put("/api/balance").contentType(MediaType.APPLICATION_JSON).content("{\"balance\":1500.50}"))
           .andExpect(status().isOk())
           .andExpect(content().json("{\"balance\":1500.50}"));

        // Partial update: liquid_reserve only -> balance must be unchanged
        mvc.perform(put("/api/balance").contentType(MediaType.APPLICATION_JSON).content("{\"liquid_reserve\":300}"))
           .andExpect(status().isOk())
           .andExpect(content().json("{\"balance\":1500.50,\"liquid_reserve\":300}"));

        // Persisted
        mvc.perform(get("/api/balance"))
           .andExpect(status().isOk())
           .andExpect(content().json("{\"balance\":1500.50,\"liquid_reserve\":300}"));
    }
}
