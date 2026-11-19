package com.gagan.order.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogProductResponse(
        Long productId,
        String productName,
        BigDecimal price,
        Integer stockQuantity,
        Boolean isActive
) {}