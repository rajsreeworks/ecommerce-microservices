package com.naveen.productservice.service;

import com.naveen.productservice.dto.ProductRequest;
import com.naveen.productservice.dto.ProductResponse;
import com.naveen.productservice.entity.Product;
import com.naveen.productservice.exception.InsufficientStockException;
import com.naveen.productservice.exception.ResourceNotFoundException;
import com.naveen.productservice.mapper.ProductMapper;
import com.naveen.productservice.repository.ProductRepository;
import com.naveen.productservice.service.impl.ProductServiceImpl;
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
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl Unit Tests")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;
    private ProductRequest request;
    private ProductResponse response;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Laptop Pro")
                .description("High performance laptop")
                .category("Electronics")
                .price(new BigDecimal("1299.99"))
                .quantity(50)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        request = ProductRequest.builder()
                .name("Laptop Pro")
                .description("High performance laptop")
                .category("Electronics")
                .price(new BigDecimal("1299.99"))
                .quantity(50)
                .build();

        response = ProductResponse.builder()
                .id(1L)
                .name("Laptop Pro")
                .description("High performance laptop")
                .category("Electronics")
                .price(new BigDecimal("1299.99"))
                .quantity(50)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    @Nested
    @DisplayName("createProduct")
    class CreateProductTests {

        @Test
        @DisplayName("should create product and return response")
        void shouldCreateProduct() {
            given(productMapper.toEntity(request)).willReturn(product);
            given(productRepository.save(product)).willReturn(product);
            given(productMapper.toResponse(product)).willReturn(response);

            ProductResponse result = productService.createProduct(request);

            assertThat(result).isEqualTo(response);
            then(productRepository).should().save(product);
        }
    }

    @Nested
    @DisplayName("getProductById")
    class GetProductByIdTests {

        @Test
        @DisplayName("should return product when found")
        void shouldReturnProductWhenFound() {
            given(productRepository.findById(1L)).willReturn(Optional.of(product));
            given(productMapper.toResponse(product)).willReturn(response);

            ProductResponse result = productService.getProductById(1L);

            assertThat(result).isEqualTo(response);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            given(productRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Product")
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("getAllProducts")
    class GetAllProductsTests {

        @Test
        @DisplayName("should return list of all products")
        void shouldReturnAllProducts() {
            given(productRepository.findAll()).willReturn(List.of(product));
            given(productMapper.toResponse(product)).willReturn(response);

            List<ProductResponse> result = productService.getAllProducts();

            assertThat(result).hasSize(1).contains(response);
        }

        @Test
        @DisplayName("should return empty list when no products exist")
        void shouldReturnEmptyList() {
            given(productRepository.findAll()).willReturn(List.of());

            List<ProductResponse> result = productService.getAllProducts();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateProduct")
    class UpdateProductTests {

        @Test
        @DisplayName("should update and return updated product")
        void shouldUpdateProduct() {
            given(productRepository.findById(1L)).willReturn(Optional.of(product));
            given(productMapper.toResponse(product)).willReturn(response);

            ProductResponse result = productService.updateProduct(1L, request);

            assertThat(result).isEqualTo(response);
            then(productMapper).should().updateEntity(product, request);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when product not found")
        void shouldThrowWhenProductNotFound() {
            given(productRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateProduct(99L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteProduct")
    class DeleteProductTests {

        @Test
        @DisplayName("should delete product when found")
        void shouldDeleteProduct() {
            given(productRepository.existsById(1L)).willReturn(true);

            productService.deleteProduct(1L);

            then(productRepository).should().deleteById(1L);
        }

        @Test
        @DisplayName("should throw when product not found")
        void shouldThrowWhenProductNotFound() {
            given(productRepository.existsById(99L)).willReturn(false);

            assertThatThrownBy(() -> productService.deleteProduct(99L))
                    .isInstanceOf(ResourceNotFoundException.class);

            then(productRepository).should(never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("reduceStock")
    class ReduceStockTests {

        @Test
        @DisplayName("should reduce stock when sufficient quantity available")
        void shouldReduceStock() {
            product.setQuantity(50);
            given(productRepository.findById(1L)).willReturn(Optional.of(product));

            productService.reduceStock(1L, 10);

            assertThat(product.getQuantity()).isEqualTo(40);
        }

        @Test
        @DisplayName("should throw InsufficientStockException when stock insufficient")
        void shouldThrowWhenInsufficientStock() {
            product.setQuantity(5);
            given(productRepository.findById(1L)).willReturn(Optional.of(product));

            assertThatThrownBy(() -> productService.reduceStock(1L, 10))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Available: 5")
                    .hasMessageContaining("Requested: 10");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when product not found")
        void shouldThrowWhenProductNotFound() {
            given(productRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.reduceStock(99L, 5))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
