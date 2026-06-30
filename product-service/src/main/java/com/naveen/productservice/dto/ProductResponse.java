package com.naveen.productservice.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class ProductResponse {

    Long id;
    String name;
    String description;
    String category;
    BigDecimal price;
    Integer quantity;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
