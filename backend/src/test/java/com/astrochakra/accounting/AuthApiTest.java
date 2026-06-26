package com.astrochakra.accounting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.astrochakra.accounting.domain.AppUser;
import com.astrochakra.accounting.repository.AppUserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuthApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    AppUserRepository users;

    @BeforeEach
    void seed() {
        users.deleteAll();
        AppUser u = new AppUser();
        u.setUsername("admin");
        u.setPasswordHash("plain123"); // legacy plaintext, as the current app stores it
        u.setDisplayName("Admin User");
        u.setEmail("admin@example.com");
        u.setRole("admin");
        u.setIsActive(true);
        u.setCreatedAt(Instant.now());
        users.save(u);
    }

    @Test
    void login_succeeds_returnsToken_andMigratesPlaintextToBcrypt() throws Exception {
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"plain123\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.token").isNotEmpty())
           .andExpect(jsonPath("$.user.username", is("admin")))
           .andExpect(jsonPath("$.user.display_name", is("Admin User")))
           .andExpect(jsonPath("$.user.role", is("admin")));

        // The stored password must now be a BCrypt hash, not the plaintext value.
        AppUser reloaded = users.findByUsername("admin").orElseThrow();
        assertThat(reloaded.getPasswordHash()).startsWith("$2");
        assertThat(reloaded.getPasswordHash()).isNotEqualTo("plain123");
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"WRONG\"}"))
           .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "bob", roles = {"REGULAR"})
    void users_endpoint_forbiddenForRegular() throws Exception {
        mvc.perform(get("/api/users")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "boss", roles = {"ADMIN"})
    void users_endpoint_allowedForAdmin() throws Exception {
        mvc.perform(get("/api/users"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].username", is("admin")));
    }
}
