package com.naveen.productservice.controller;

import com.naveen.productservice.dto.ProductRequest;
import com.naveen.productservice.dto.ProductResponse;
import com.naveen.productservice.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product", description = "Product management and inventory APIs")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "Create a new product")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<ProductResponse> getProductById(
            @Parameter(description = "Product ID") @PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping
    @Operation(summary = "List all products, optionally filtered by category")
    public ResponseEntity<List<ProductResponse>> getAllProducts(
            @Parameter(description = "Filter by category")
            @RequestParam(required = false) String category) {

        List<ProductResponse> products = category != null
                ? productService.getProductsByCategory(category)
                : productService.getAllProducts();

        return ResponseEntity.ok(products);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated"),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product deleted"),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reduce-stock")
    @Operation(summary = "Reduce product stock — called internally by Order Service")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Stock reduced"),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "Insufficient stock", content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<Void> reduceStock(
            @PathVariable Long id,
            @Parameter(description = "Quantity to reduce")
            @RequestParam @Min(1) int quantity) {
        productService.reduceStock(id, quantity);
        return ResponseEntity.noContent().build();
    }
}
