package com.gagan.catalog.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gagan.catalog.dto.ProductRequest;
import com.gagan.catalog.dto.ProductResponse;
import com.gagan.catalog.dto.ReleaseStockRequest;
import com.gagan.catalog.dto.StockUpdateRequest;
import com.gagan.catalog.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts(
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(productService.getAllActiveProducts(categoryId));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getProductById(productId));
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductResponse> getProductBySku(@PathVariable String sku) {
        return ResponseEntity.ok(productService.getProductBySku(sku));
    }
    @PostMapping("/{productId}/reserve-stock")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ProductResponse> reserveStock(
            @PathVariable Long productId,
            @Valid @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(productService.reserveStock(productId, request));
    }

    @PostMapping("/{productId}/release-stock")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ProductResponse> releaseStock(
            @PathVariable Long productId,
            @Valid @RequestBody ReleaseStockRequest request) {
        return ResponseEntity.ok(productService.releaseStock(productId, request));
    }

    @PostMapping("/{productId}/confirm-stock")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ProductResponse> confirmStock(
            @PathVariable Long productId,
            @Valid @RequestBody ReleaseStockRequest request) {
        return ResponseEntity.ok(productService.confirmStock(productId, request));
    }
}