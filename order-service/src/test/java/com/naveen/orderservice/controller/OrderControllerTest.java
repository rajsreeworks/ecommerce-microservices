package com.naveen.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naveen.orderservice.dto.OrderRequest;
import com.naveen.orderservice.dto.OrderResponse;
import com.naveen.orderservice.entity.Order.OrderStatus;
import com.naveen.orderservice.exception.InsufficientStockException;
import com.naveen.orderservice.exception.ResourceNotFoundException;
import com.naveen.orderservice.service.OrderService;
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
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@DisplayName("OrderController Tests")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    private OrderRequest validRequest;
    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        validRequest = OrderRequest.builder()
                .productId(1L)
                .quantity(2)
                .build();

        orderResponse = OrderResponse.builder()
                .id(1L)
                .productId(1L)
                .quantity(2)
                .totalPrice(new BigDecimal("2599.98"))
                .status(OrderStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/orders")
    class CreateOrderTests {

        @Test
        @DisplayName("should return 201 when order is created")
        void shouldReturn201() throws Exception {
            given(orderService.createOrder(any(OrderRequest.class))).willReturn(orderResponse);

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.status").value("CONFIRMED"))
                    .andExpect(jsonPath("$.totalPrice").value(2599.98));
        }

        @Test
        @DisplayName("should return 400 when quantity is 0")
        void shouldReturn400WhenQuantityZero() throws Exception {
            OrderRequest invalid = OrderRequest.builder().productId(1L).quantity(0).build();

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.quantity").exists());
        }

        @Test
        @DisplayName("should return 409 when stock is insufficient")
        void shouldReturn409WhenInsufficientStock() throws Exception {
            given(orderService.createOrder(any(OrderRequest.class)))
                    .willThrow(new InsufficientStockException(1L, 1, 5));

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }

        @Test
        @DisplayName("should return 404 when product not found")
        void shouldReturn404WhenProductNotFound() throws Exception {
            given(orderService.createOrder(any(OrderRequest.class)))
                    .willThrow(new ResourceNotFoundException("Product", "id", 1L));

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/orders/{id}")
    class GetOrderByIdTests {

        @Test
        @DisplayName("should return 200 with order")
        void shouldReturn200() throws Exception {
            given(orderService.getOrderById(1L)).willReturn(orderResponse);

            mockMvc.perform(get("/api/v1/orders/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L));
        }

        @Test
        @DisplayName("should return 404 when not found")
        void shouldReturn404() throws Exception {
            given(orderService.getOrderById(99L))
                    .willThrow(new ResourceNotFoundException("Order", "id", 99L));

            mockMvc.perform(get("/api/v1/orders/99"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/orders")
    class GetAllOrdersTests {

        @Test
        @DisplayName("should return list of orders")
        void shouldReturnList() throws Exception {
            given(orderService.getAllOrders()).willReturn(List.of(orderResponse));

            mockMvc.perform(get("/api/v1/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1L));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/orders/{id}/cancel")
    class CancelOrderTests {

        @Test
        @DisplayName("should return 200 with cancelled order")
        void shouldReturn200OnCancel() throws Exception {
            OrderResponse cancelled = OrderResponse.builder()
                    .id(1L).productId(1L).quantity(2)
                    .totalPrice(new BigDecimal("2599.98"))
                    .status(OrderStatus.CANCELLED)
                    .createdAt(LocalDateTime.now())
                    .build();

            given(orderService.cancelOrder(1L)).willReturn(cancelled);

            mockMvc.perform(patch("/api/v1/orders/1/cancel"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }
    }
}
