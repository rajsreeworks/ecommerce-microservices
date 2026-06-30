package com.naveen.orderservice.service;

import com.naveen.orderservice.client.ProductServiceClient;
import com.naveen.orderservice.dto.OrderRequest;
import com.naveen.orderservice.dto.OrderResponse;
import com.naveen.orderservice.dto.ProductResponse;
import com.naveen.orderservice.entity.Order;
import com.naveen.orderservice.entity.Order.OrderStatus;
import com.naveen.orderservice.exception.InsufficientStockException;
import com.naveen.orderservice.exception.ProductServiceException;
import com.naveen.orderservice.exception.ResourceNotFoundException;
import com.naveen.orderservice.mapper.OrderMapper;
import com.naveen.orderservice.repository.OrderRepository;
import com.naveen.orderservice.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl Unit Tests")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ProductServiceClient productServiceClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    private ProductResponse availableProduct;
    private OrderRequest validRequest;
    private Order savedOrder;
    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        availableProduct = new ProductResponse(1L, "Laptop", new BigDecimal("1299.99"), 50,
                LocalDateTime.now(), LocalDateTime.now());

        validRequest = OrderRequest.builder()
                .productId(1L)
                .quantity(2)
                .build();

        savedOrder = Order.builder()
                .id(1L)
                .productId(1L)
                .quantity(2)
                .totalPrice(new BigDecimal("2599.98"))
                .status(OrderStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();

        orderResponse = OrderResponse.builder()
                .id(1L)
                .productId(1L)
                .quantity(2)
                .totalPrice(new BigDecimal("2599.98"))
                .status(OrderStatus.CONFIRMED)
                .createdAt(savedOrder.getCreatedAt())
                .build();
    }

    @Nested
    @DisplayName("createOrder")
    class CreateOrderTests {

        @Test
        @DisplayName("should create order when product is available and stock is sufficient")
        void shouldCreateOrder() {
            given(productServiceClient.getProductById(1L)).willReturn(availableProduct);
            willDoNothing().given(productServiceClient).reduceStock(1L, 2);
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
            given(orderMapper.toResponse(savedOrder)).willReturn(orderResponse);

            OrderResponse result = orderService.createOrder(validRequest);

            assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(result.getTotalPrice()).isEqualByComparingTo(new BigDecimal("2599.98"));
            then(productServiceClient).should().reduceStock(1L, 2);
        }

        @Test
        @DisplayName("should throw InsufficientStockException when stock is too low")
        void shouldThrowWhenStockInsufficient() {
            ProductResponse lowStockProduct = new ProductResponse(
                    1L, "Laptop", new BigDecimal("1299.99"), 1,
                    LocalDateTime.now(), LocalDateTime.now());

            given(productServiceClient.getProductById(1L)).willReturn(lowStockProduct);

            assertThatThrownBy(() -> orderService.createOrder(
                    OrderRequest.builder().productId(1L).quantity(5).build()))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Available: 1")
                    .hasMessageContaining("Requested: 5");

            then(productServiceClient).shouldHaveNoMoreInteractions();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when product does not exist")
        void shouldThrowWhenProductNotFound() {
            // Simulates what ProductServiceClientFallbackFactory throws on FeignException.NotFound
            given(productServiceClient.getProductById(99L))
                    .willThrow(new ResourceNotFoundException("Product", "id", 99L));

            assertThatThrownBy(() -> orderService.createOrder(
                    OrderRequest.builder().productId(99L).quantity(1).build()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Product");
        }

        @Test
        @DisplayName("should throw ProductServiceException when Product Service is unreachable")
        void shouldThrowWhenProductServiceDown() {
            // Simulates what ProductServiceClientFallbackFactory throws on connection failure
            given(productServiceClient.getProductById(1L))
                    .willThrow(new ProductServiceException("Unable to reach Product Service. Please try again later."));

            assertThatThrownBy(() -> orderService.createOrder(validRequest))
                    .isInstanceOf(ProductServiceException.class)
                    .hasMessageContaining("Product Service");
        }
    }

    @Nested
    @DisplayName("getOrderById")
    class GetOrderByIdTests {

        @Test
        @DisplayName("should return order when found")
        void shouldReturnOrder() {
            given(orderRepository.findById(1L)).willReturn(Optional.of(savedOrder));
            given(orderMapper.toResponse(savedOrder)).willReturn(orderResponse);

            OrderResponse result = orderService.getOrderById(1L);

            assertThat(result).isEqualTo(orderResponse);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when order not found")
        void shouldThrowWhenNotFound() {
            given(orderRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Order");
        }
    }

    @Nested
    @DisplayName("getAllOrders")
    class GetAllOrdersTests {

        @Test
        @DisplayName("should return all orders")
        void shouldReturnAllOrders() {
            given(orderRepository.findAll()).willReturn(List.of(savedOrder));
            given(orderMapper.toResponse(savedOrder)).willReturn(orderResponse);

            List<OrderResponse> result = orderService.getAllOrders();

            assertThat(result).hasSize(1).contains(orderResponse);
        }
    }

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrderTests {

        @Test
        @DisplayName("should cancel a confirmed order")
        void shouldCancelOrder() {
            given(orderRepository.findById(1L)).willReturn(Optional.of(savedOrder));
            given(orderMapper.toResponse(savedOrder)).willReturn(
                    OrderResponse.builder()
                            .id(1L).productId(1L).quantity(2)
                            .totalPrice(new BigDecimal("2599.98"))
                            .status(OrderStatus.CANCELLED)
                            .createdAt(savedOrder.getCreatedAt())
                            .build());

            OrderResponse result = orderService.cancelOrder(1L);

            assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("should throw IllegalStateException when order is already cancelled")
        void shouldThrowWhenAlreadyCancelled() {
            savedOrder.setStatus(OrderStatus.CANCELLED);
            given(orderRepository.findById(1L)).willReturn(Optional.of(savedOrder));

            assertThatThrownBy(() -> orderService.cancelOrder(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already cancelled");
        }
    }
}
