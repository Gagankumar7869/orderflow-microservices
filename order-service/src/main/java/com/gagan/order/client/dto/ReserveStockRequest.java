package com.gagan.order.client.dto;

public record ReserveStockRequest(
        String orderReference,
        Integer quantity
) {}