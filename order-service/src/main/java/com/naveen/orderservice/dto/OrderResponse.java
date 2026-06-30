package com.naveen.orderservice.dto;

import com.naveen.orderservice.entity.Order.OrderStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class OrderResponse {

    Long id;
    Long productId;
    Integer quantity;
    BigDecimal totalPrice;
    OrderStatus status;
    LocalDateTime createdAt;
}
