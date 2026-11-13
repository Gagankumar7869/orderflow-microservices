package com.gagan.catalog.service;

import com.gagan.catalog.dto.ProductRequest;
import com.gagan.catalog.dto.ProductResponse;
import com.gagan.catalog.dto.ReleaseStockRequest;
import com.gagan.catalog.dto.StockUpdateRequest;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);
    List<ProductResponse> getAllActiveProducts(Long categoryId);
    ProductResponse getProductById(Long productId);
    ProductResponse getProductBySku(String sku);
    ProductResponse reserveStock(Long productId, StockUpdateRequest request);
    ProductResponse releaseStock(Long productId, ReleaseStockRequest request);
    ProductResponse confirmStock(Long productId, ReleaseStockRequest request);
}