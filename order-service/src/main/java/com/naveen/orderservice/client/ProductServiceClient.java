package com.naveen.orderservice.client;

import com.naveen.orderservice.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "product-service",
        url = "${product.service.url:}",
        fallbackFactory = ProductServiceClientFallbackFactory.class
)
public interface ProductServiceClient {

    @GetMapping("/api/v1/products/{id}")
    ProductResponse getProductById(@PathVariable Long id);

    @PatchMapping("/api/v1/products/{id}/reduce-stock")
    void reduceStock(@PathVariable Long id, @RequestParam int quantity);
}
