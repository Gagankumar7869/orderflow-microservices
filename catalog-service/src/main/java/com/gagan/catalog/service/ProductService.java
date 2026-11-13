package com.gagan.catalog.service;

import com.gagan.catalog.dto.ProductRequest;
import com.gagan.catalog.dto.ProductResponse;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);
    List<ProductResponse> getAllActiveProducts(Long categoryId);
    ProductResponse getProductById(Long productId);
    ProductResponse getProductBySku(String sku);
    ProductResponse reserveStock(Long productId, int quantity);
    ProductResponse releaseStock(Long productId, int quantity);
}