package com.naveen.orderservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.naveen.orderservice.dto.OrderRequest;
import com.naveen.orderservice.dto.OrderResponse;
import com.naveen.orderservice.entity.Order.OrderStatus;
import com.naveen.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 9090)
@TestPropertySource(properties = "product.service.url=http://localhost:9090")
@DisplayName("Order Service Integration Tests")
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    private static final String PRODUCT_JSON = """
            {
              "id": 1,
              "name": "Laptop Pro",
              "price": 1299.99,
              "quantity": 50,
              "createdAt": "2024-01-01T10:00:00",
              "updatedAt": "2024-01-01T10:00:00"
            }
            """;

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        WireMock.reset();
    }

    @Test
    @DisplayName("should create order when product is available")
    void shouldCreateOrder() throws Exception {
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/v1/products/1"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(PRODUCT_JSON)));

        WireMock.stubFor(WireMock.patch(WireMock.urlPathEqualTo("/api/v1/products/1/reduce-stock"))
                .willReturn(WireMock.aResponse().withStatus(204)));

        OrderRequest request = OrderRequest.builder().productId(1L).quantity(2).build();

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("CONFIRMED"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalPrice").value(2599.98))
                .andReturn();

        OrderResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), OrderResponse.class);
        assertThat(response.getId()).isNotNull();
        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("should return 404 when product does not exist")
    void shouldReturn404WhenProductNotFound() throws Exception {
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/v1/products/99"))
                .willReturn(WireMock.aResponse().withStatus(404)));

        OrderRequest request = OrderRequest.builder().productId(99L).quantity(1).build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    @DisplayName("should return 409 when stock is insufficient")
    void shouldReturn409WhenInsufficientStock() throws Exception {
        String lowStockProduct = """
                {
                  "id": 1,
                  "name": "Laptop Pro",
                  "price": 1299.99,
                  "quantity": 1,
                  "createdAt": "2024-01-01T10:00:00",
                  "updatedAt": "2024-01-01T10:00:00"
                }
                """;

        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/v1/products/1"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(lowStockProduct)));

        OrderRequest request = OrderRequest.builder().productId(1L).quantity(10).build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("Insufficient stock")));

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    @DisplayName("should cancel a confirmed order")
    void shouldCancelOrder() throws Exception {
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/v1/products/1"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(PRODUCT_JSON)));

        WireMock.stubFor(WireMock.patch(WireMock.urlPathEqualTo("/api/v1/products/1/reduce-stock"))
                .willReturn(WireMock.aResponse().withStatus(204)));

        OrderRequest request = OrderRequest.builder().productId(1L).quantity(1).build();

        MvcResult created = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();

        Long orderId = objectMapper.readValue(
                created.getResponse().getContentAsString(), OrderResponse.class).getId();

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/orders/" + orderId + "/cancel"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("should return 503 when Product Service returns server error")
    void shouldReturn503WhenProductServiceDown() throws Exception {
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/v1/products/1"))
                .willReturn(WireMock.aResponse().withStatus(500)));

        OrderRequest request = OrderRequest.builder().productId(1L).quantity(1).build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isServiceUnavailable());
    }
}
