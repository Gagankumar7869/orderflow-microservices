package com.gagan.order.dto;

import com.gagan.order.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long orderId,
        String orderReference,
        Long userId,
        String customerEmail,
        String shippingAddress,
        BigDecimal totalAmount,
        OrderStatus orderStatus,
        List<OrderItemResponse> items,
        LocalDateTime createdAt
) {}