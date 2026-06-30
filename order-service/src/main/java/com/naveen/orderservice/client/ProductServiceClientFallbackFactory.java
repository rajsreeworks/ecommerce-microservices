package com.naveen.orderservice.client;

import com.naveen.orderservice.dto.ProductResponse;
import com.naveen.orderservice.exception.ProductServiceException;
import com.naveen.orderservice.exception.ResourceNotFoundException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProductServiceClientFallbackFactory implements FallbackFactory<ProductServiceClient> {

    @Override
    public ProductServiceClient create(Throwable cause) {
        return new ProductServiceClient() {

            @Override
            public ProductResponse getProductById(Long id) {
                if (cause instanceof FeignException.NotFound) {
                    throw new ResourceNotFoundException("Product", "id", id);
                }
                if (cause instanceof CallNotPermittedException) {
                    log.error("Circuit breaker OPEN — product-service calls blocked: {}", cause.getMessage());
                    throw new ProductServiceException(
                            "Product Service is temporarily unavailable. Please try again later.", cause);
                }
                log.error("Fallback triggered for getProductById({}): {}", id, cause.getMessage());
                throw new ProductServiceException(
                        "Unable to reach Product Service. Please try again later.", cause);
            }

            @Override
            public void reduceStock(Long id, int quantity) {
                if (cause instanceof CallNotPermittedException) {
                    log.error("Circuit breaker OPEN — reduceStock blocked: {}", cause.getMessage());
                    throw new ProductServiceException(
                            "Product Service is temporarily unavailable. Please try again later.", cause);
                }
                log.error("Fallback triggered for reduceStock({}, {}): {}", id, quantity, cause.getMessage());
                throw new ProductServiceException(
                        "Failed to update product inventory. Please try again later.", cause);
            }
        };
    }
}
