package com.naveen.productservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naveen.productservice.dto.ProductRequest;
import com.naveen.productservice.dto.ProductResponse;
import com.naveen.productservice.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Product Service Integration Tests")
class ProductIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @AfterEach
    void tearDown() {
        productRepository.deleteAll();
    }

    private ProductRequest buildRequest(String name, String category, BigDecimal price, int qty) {
        return ProductRequest.builder()
                .name(name)
                .description("Test description")
                .category(category)
                .price(price)
                .quantity(qty)
                .build();
    }

    @Test
    @DisplayName("should create, retrieve, update, and delete a product")
    void fullCrudLifecycle() throws Exception {
        // CREATE
        ProductRequest createRequest = buildRequest("Laptop Pro", "Electronics", new BigDecimal("1299.99"), 50);

        MvcResult createResult = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Laptop Pro"))
                .andReturn();

        ProductResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), ProductResponse.class);
        Long productId = created.getId();

        // READ
        mockMvc.perform(get("/api/v1/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.quantity").value(50));

        // UPDATE
        ProductRequest updateRequest = buildRequest("Laptop Pro X", "Electronics", new BigDecimal("1499.99"), 40);

        mockMvc.perform(put("/api/v1/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop Pro X"))
                .andExpect(jsonPath("$.price").value(1499.99));

        // REDUCE STOCK
        mockMvc.perform(patch("/api/v1/products/" + productId + "/reduce-stock")
                        .param("quantity", "10"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/products/" + productId))
                .andExpect(jsonPath("$.quantity").value(30));

        // DELETE
        mockMvc.perform(delete("/api/v1/products/" + productId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/products/" + productId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should return 409 when reducing stock beyond available quantity")
    void shouldReturn409OnInsufficientStock() throws Exception {
        ProductRequest request = buildRequest("Widget", "Tools", new BigDecimal("9.99"), 5);

        MvcResult result = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        ProductResponse created = objectMapper.readValue(
                result.getResponse().getContentAsString(), ProductResponse.class);

        mockMvc.perform(patch("/api/v1/products/" + created.getId() + "/reduce-stock")
                        .param("quantity", "100"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Insufficient stock")));
    }

    @Test
    @DisplayName("should return products filtered by category")
    void shouldFilterByCategory() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildRequest("Phone", "Electronics", new BigDecimal("799.99"), 20))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildRequest("Shirt", "Clothing", new BigDecimal("29.99"), 100))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/products").param("category", "Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].category").value("Electronics"));
    }

    @Test
    @DisplayName("should persist createdAt and updatedAt timestamps")
    void shouldPersistAuditTimestamps() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildRequest("Widget", "Tools", new BigDecimal("5.99"), 10))))
                .andExpect(status().isCreated())
                .andReturn();

        ProductResponse created = objectMapper.readValue(
                result.getResponse().getContentAsString(), ProductResponse.class);

        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getUpdatedAt()).isNotNull();
    }
}
