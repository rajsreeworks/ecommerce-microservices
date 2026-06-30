package com.naveen.productservice.service.impl;

import com.naveen.productservice.dto.ProductRequest;
import com.naveen.productservice.dto.ProductResponse;
import com.naveen.productservice.entity.Product;
import com.naveen.productservice.exception.InsufficientStockException;
import com.naveen.productservice.exception.ResourceNotFoundException;
import com.naveen.productservice.mapper.ProductMapper;
import com.naveen.productservice.repository.ProductRepository;
import com.naveen.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating product with name: {}", request.getName());
        Product product = productMapper.toEntity(request);
        Product saved = productRepository.save(product);
        log.info("Product created with id: {}", saved.getId());
        return productMapper.toResponse(saved);
    }

    @Override
    public ProductResponse getProductById(Long id) {
        log.debug("Fetching product with id: {}", id);
        return productRepository.findById(id)
                .map(productMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    @Override
    public List<ProductResponse> getAllProducts() {
        log.debug("Fetching all products");
        return productRepository.findAll()
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    public List<ProductResponse> getProductsByCategory(String category) {
        log.debug("Fetching products for category: {}", category);
        return productRepository.findByCategory(category)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("Updating product with id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        productMapper.updateEntity(product, request);
        // No explicit save() needed — Hibernate detects dirty entity within transaction
        log.info("Product updated with id: {}", id);
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Deleting product with id: {}", id);
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", "id", id);
        }
        productRepository.deleteById(id);
        log.info("Product deleted with id: {}", id);
    }

    @Override
    @Transactional
    public void reduceStock(Long id, int quantity) {
        log.info("Reducing stock for product id: {} by: {}", id, quantity);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        if (product.getQuantity() < quantity) {
            throw new InsufficientStockException(id, product.getQuantity(), quantity);
        }

        product.setQuantity(product.getQuantity() - quantity);
        log.info("Stock reduced. Product id: {}, remaining quantity: {}", id, product.getQuantity());
    }
}
