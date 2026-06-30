package com.naveen.orderservice.repository;

import com.naveen.orderservice.entity.Order;
import com.naveen.orderservice.entity.Order.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByProductId(Long productId);

    List<Order> findByStatus(OrderStatus status);
}
