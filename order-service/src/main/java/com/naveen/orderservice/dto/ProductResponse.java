package com.naveen.orderservice.dto;

import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mirror of product-service's ProductResponse.
 * Only fields this service needs are declared — extra fields in the JSON are ignored by Jackson.
 */
@Value
public class ProductResponse {

    Long id;
    String name;
    BigDecimal price;
    Integer quantity;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
