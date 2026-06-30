package com.naveen.productservice.service;

import com.naveen.productservice.dto.ProductRequest;
import com.naveen.productservice.dto.ProductResponse;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductRequest request);

    ProductResponse getProductById(Long id);

    List<ProductResponse> getAllProducts();

    List<ProductResponse> getProductsByCategory(String category);

    ProductResponse updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);

    void reduceStock(Long id, int quantity);
}
