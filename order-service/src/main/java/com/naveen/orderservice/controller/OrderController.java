package com.naveen.orderservice.controller;

import com.naveen.orderservice.dto.OrderRequest;
import com.naveen.orderservice.dto.OrderResponse;
import com.naveen.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order", description = "Order management APIs")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place a new order")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order placed"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "Insufficient stock", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "503", description = "Product Service unavailable", content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an order by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(responseCode = "404", description = "Order not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<OrderResponse> getOrderById(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping
    @Operation(summary = "List all orders")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order cancelled"),
            @ApiResponse(responseCode = "404", description = "Order not found", content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<OrderResponse> cancelOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }
}
