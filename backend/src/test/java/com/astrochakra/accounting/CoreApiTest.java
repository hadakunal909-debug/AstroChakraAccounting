package com.astrochakra.accounting;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@WithMockUser(username = "tester", roles = {"ADMIN"})
class CoreApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void project_create_returnsSnakeCaseFields() throws Exception {
        mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"PRJ-1\",\"name\":\"Temple\",\"fixed_budget\":5000,\"allocated\":5000,\"color\":\"#ffffff\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.code", is("PRJ-1")))
           .andExpect(jsonPath("$.fixed_budget", is(5000)))
           .andExpect(jsonPath("$.allocated", is(5000)));
    }

    @Test
    void transaction_create_edit_settle() throws Exception {
        // Create a debit; defaults applied (settled=false, is_reversal=false), snake_case keys
        String body = mvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2026-04-15\",\"person\":\"Ravi\",\"amount\":250,\"kind\":\"general_expense\",\"category\":\"Travel\",\"note\":\"Cab\",\"fund_source\":\"available\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.fund_source", is("available")))
           .andExpect(jsonPath("$.is_reversal", is(false)))
           .andExpect(jsonPath("$.settled", is(false)))
           .andExpect(content().json("{\"amount\":250}"))
           .andReturn().getResponse().getContentAsString();
        String id = String.valueOf((Object) JsonPath.read(body, "$.id"));

        // It appears in the list (ids are numeric now)
        mvc.perform(get("/api/transactions"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[*].id", hasItem(Integer.valueOf(id))));

        // Edit tags only — assign a project + change category; amount must NOT change
        mvc.perform(patch("/api/transactions/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2026-04-16\",\"person\":\"Ravi\",\"category\":\"Food & Meals\",\"project_code\":\"PRJ-1\",\"note\":\"Lunch\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.category", is("Food & Meals")))
           .andExpect(jsonPath("$.project_code", is("PRJ-1")))
           .andExpect(content().json("{\"amount\":250}"));

        // Settle flips the flag
        mvc.perform(put("/api/transactions/" + id + "/settle"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.settled", is(true)));
    }

    @Test
    void expenseCategory_create_and_list() throws Exception {
        mvc.perform(post("/api/expense-categories").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Pooja Supplies\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.name", is("Pooja Supplies")));

        mvc.perform(get("/api/expense-categories"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[*].name", hasItem("Pooja Supplies")));
    }
}
