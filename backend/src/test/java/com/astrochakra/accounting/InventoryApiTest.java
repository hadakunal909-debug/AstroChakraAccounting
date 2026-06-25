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
import com.astrochakra.accounting.repository.BalanceRepository;
import com.astrochakra.accounting.repository.InventoryProductRepository;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@WithMockUser(username = "tester", roles = {"ADMIN"})
class InventoryApiTest {

    @Autowired MockMvc mvc;
    @Autowired BalanceRepository balances;
    @Autowired InventoryProductRepository products;

    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    @BeforeEach
    void reset() {
        products.deleteAll();
        balances.deleteAll();
        Balance b = new Balance();
        b.setId(UUID.randomUUID());
        b.setBalance(new BigDecimal("10000"));
        b.setLiquidReserve(BigDecimal.ZERO);
        balances.save(b);
    }

    @Test
    void stockIn_increasesStock_andReducesBalance() throws Exception {
        String body = mvc.perform(post("/api/inventory/products").contentType(JSON)
                .content("{\"name\":\"Incense\",\"unit\":\"pcs\",\"buying_price\":50,\"selling_price\":80,\"min_stock\":5,\"tax_percent\":18,\"product_type\":\"Product\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.name", is("Incense")))
           .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        mvc.perform(post("/api/inventory/stock-movement").contentType(JSON)
                .content("{\"product_id\":\"" + id + "\",\"type\":\"stock_in\",\"quantity\":10,\"unit_cost\":50,\"total_cost\":500,\"fund_source\":\"available\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.type", is("stock_in")));

        // Stock went up by 10
        String prodsJson = mvc.perform(get("/api/inventory/products")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        BigDecimal stock = new BigDecimal(JsonPath.read(prodsJson, "$[0].current_stock").toString());
        assertThat(stock).isEqualByComparingTo("10");

        // Bank balance dropped by total cost (500)
        mvc.perform(get("/api/balance")).andExpect(content().json("{\"balance\":9500}"));
    }
}
