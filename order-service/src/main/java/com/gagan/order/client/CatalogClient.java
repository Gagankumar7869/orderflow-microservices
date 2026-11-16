package com.gagan.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "catalog-client", url = "${catalog.service.url}")
public interface CatalogClient {

    @PostMapping("/api/v1/products/{productId}/reserve-stock")
    ResponseEntity<Void> reserveStock(
        @RequestHeader("Authorization") String token,
        @PathVariable("productId") Long productId,
        @RequestBody Map<String, Object> stockRequest
    );

    @PostMapping("/api/v1/products/{productId}/release-stock")
    ResponseEntity<Void> releaseStock(
        @RequestHeader("Authorization") String token,
        @PathVariable("productId") Long productId,
        @RequestBody Map<String, Object> releaseRequest
    );
}