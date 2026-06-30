package com.naveen.orderservice.service.impl;

import com.naveen.orderservice.client.ProductServiceClient;
import com.naveen.orderservice.dto.OrderRequest;
import com.naveen.orderservice.dto.OrderResponse;
import com.naveen.orderservice.dto.ProductResponse;
import com.naveen.orderservice.entity.Order;
import com.naveen.orderservice.entity.Order.OrderStatus;
import com.naveen.orderservice.exception.InsufficientStockException;
import com.naveen.orderservice.exception.ResourceNotFoundException;
import com.naveen.orderservice.mapper.OrderMapper;
import com.naveen.orderservice.repository.OrderRepository;
import com.naveen.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProductServiceClient productServiceClient;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for productId: {}, quantity: {}", request.getProductId(), request.getQuantity());

        // 1. Verify product exists and fetch its price
        ProductResponse product = fetchProduct(request.getProductId());

        // 2. Verify sufficient stock
        if (product.getQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(
                    request.getProductId(), product.getQuantity(), request.getQuantity());
        }

        // 3. Reduce stock in Product Service
        reduceProductStock(request.getProductId(), request.getQuantity());

        // 4. Compute total and persist order
        BigDecimal totalPrice = product.getPrice()
                .multiply(BigDecimal.valueOf(request.getQuantity()));

        Order order = Order.builder()
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .totalPrice(totalPrice)
                .status(OrderStatus.CONFIRMED)
                .build();

        Order saved = orderRepository.save(order);
        log.info("Order created with id: {}", saved.getId());

        return orderMapper.toResponse(saved);
    }

    @Override
    public OrderResponse getOrderById(Long id) {
        log.debug("Fetching order with id: {}", id);
        return orderRepository.findById(id)
                .map(orderMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        log.debug("Fetching all orders");
        return orderRepository.findAll()
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long id) {
        log.info("Cancelling order with id: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order " + id + " is already cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        log.info("Order {} cancelled", id);

        return orderMapper.toResponse(order);
    }

    // ----------------------------------------------------------------
    // Private helpers — exception translation is handled by the
    // fallback factory; these methods stay clean of Feign internals
    // ----------------------------------------------------------------

    private ProductResponse fetchProduct(Long productId) {
        return productServiceClient.getProductById(productId);
    }

    private void reduceProductStock(Long productId, int quantity) {
        productServiceClient.reduceStock(productId, quantity);
    }
}
