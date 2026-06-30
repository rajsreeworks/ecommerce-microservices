package com.naveen.productservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naveen.productservice.dto.ProductRequest;
import com.naveen.productservice.dto.ProductResponse;
import com.naveen.productservice.exception.GlobalExceptionHandler;
import com.naveen.productservice.exception.ResourceNotFoundException;
import com.naveen.productservice.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProductController.class)
@DisplayName("ProductController Tests")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    private ProductRequest validRequest;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        validRequest = ProductRequest.builder()
                .name("Laptop Pro")
                .description("High performance laptop")
                .category("Electronics")
                .price(new BigDecimal("1299.99"))
                .quantity(50)
                .build();

        productResponse = ProductResponse.builder()
                .id(1L)
                .name("Laptop Pro")
                .description("High performance laptop")
                .category("Electronics")
                .price(new BigDecimal("1299.99"))
                .quantity(50)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/products")
    class CreateProductTests {

        @Test
        @DisplayName("should return 201 and product response when request is valid")
        void shouldReturn201WhenValid() throws Exception {
            given(productService.createProduct(any(ProductRequest.class))).willReturn(productResponse);

            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Laptop Pro"))
                    .andExpect(jsonPath("$.price").value(1299.99));
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        void shouldReturn400WhenNameIsBlank() throws Exception {
            ProductRequest invalidRequest = ProductRequest.builder()
                    .name("")
                    .category("Electronics")
                    .price(new BigDecimal("1299.99"))
                    .quantity(10)
                    .build();

            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.name").exists());
        }

        @Test
        @DisplayName("should return 400 when price is zero")
        void shouldReturn400WhenPriceIsZero() throws Exception {
            ProductRequest invalidRequest = ProductRequest.builder()
                    .name("Laptop")
                    .category("Electronics")
                    .price(BigDecimal.ZERO)
                    .quantity(10)
                    .build();

            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.price").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products/{id}")
    class GetProductByIdTests {

        @Test
        @DisplayName("should return 200 and product when found")
        void shouldReturn200WhenFound() throws Exception {
            given(productService.getProductById(1L)).willReturn(productResponse);

            mockMvc.perform(get("/api/v1/products/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Laptop Pro"));
        }

        @Test
        @DisplayName("should return 404 when product not found")
        void shouldReturn404WhenNotFound() throws Exception {
            given(productService.getProductById(99L))
                    .willThrow(new ResourceNotFoundException("Product", "id", 99L));

            mockMvc.perform(get("/api/v1/products/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products")
    class GetAllProductsTests {

        @Test
        @DisplayName("should return 200 with all products")
        void shouldReturnAllProducts() throws Exception {
            given(productService.getAllProducts()).willReturn(List.of(productResponse));

            mockMvc.perform(get("/api/v1/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value(1L));
        }

        @Test
        @DisplayName("should return filtered products when category param provided")
        void shouldReturnFilteredByCategory() throws Exception {
            given(productService.getProductsByCategory("Electronics")).willReturn(List.of(productResponse));

            mockMvc.perform(get("/api/v1/products").param("category", "Electronics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].category").value("Electronics"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/products/{id}")
    class UpdateProductTests {

        @Test
        @DisplayName("should return 200 with updated product")
        void shouldReturn200OnUpdate() throws Exception {
            given(productService.updateProduct(eq(1L), any(ProductRequest.class))).willReturn(productResponse);

            mockMvc.perform(put("/api/v1/products/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L));
        }

        @Test
        @DisplayName("should return 404 when product not found")
        void shouldReturn404WhenNotFound() throws Exception {
            given(productService.updateProduct(eq(99L), any(ProductRequest.class)))
                    .willThrow(new ResourceNotFoundException("Product", "id", 99L));

            mockMvc.perform(put("/api/v1/products/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/products/{id}")
    class DeleteProductTests {

        @Test
        @DisplayName("should return 204 on successful deletion")
        void shouldReturn204OnDelete() throws Exception {
            willDoNothing().given(productService).deleteProduct(1L);

            mockMvc.perform(delete("/api/v1/products/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 404 when product not found")
        void shouldReturn404WhenNotFound() throws Exception {
            willThrow(new ResourceNotFoundException("Product", "id", 99L))
                    .given(productService).deleteProduct(99L);

            mockMvc.perform(delete("/api/v1/products/99"))
                    .andExpect(status().isNotFound());
        }
    }
}
