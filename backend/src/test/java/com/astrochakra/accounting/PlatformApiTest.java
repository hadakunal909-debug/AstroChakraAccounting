package com.astrochakra.accounting;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@WithMockUser(username = "tester", roles = {"ADMIN"})
class PlatformApiTest {

    @Autowired MockMvc mvc;
    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    @Test
    void companyInfo_upsert() throws Exception {
        mvc.perform(put("/api/company").contentType(JSON)
                .content("{\"name\":\"AstroChakra Pvt Ltd\",\"gstin\":\"29ABCDE1234F1Z5\",\"email\":\"hi@astrochakra.co\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.name", is("AstroChakra Pvt Ltd")))
           .andExpect(jsonPath("$.gstin", is("29ABCDE1234F1Z5")));
        mvc.perform(get("/api/company"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.name", is("AstroChakra Pvt Ltd")));
    }

    @Test
    void activityLog_writeAndRead() throws Exception {
        mvc.perform(post("/api/activity-log").contentType(JSON)
                .content("{\"user_name\":\"Admin\",\"user_role\":\"admin\",\"action\":\"login\",\"description\":\"Logged in\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.user_name", is("Admin")))
           .andExpect(jsonPath("$.action", is("login")));
        mvc.perform(get("/api/activity-log?limit=5"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[*].description", hasItem("Logged in")));
    }
}
