package com.naveen.orderservice.service;

import com.naveen.orderservice.dto.OrderRequest;
import com.naveen.orderservice.dto.OrderResponse;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(OrderRequest request);

    OrderResponse getOrderById(Long id);

    List<OrderResponse> getAllOrders();

    OrderResponse cancelOrder(Long id);
}
