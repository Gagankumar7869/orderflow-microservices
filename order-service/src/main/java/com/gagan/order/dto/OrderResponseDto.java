package com.gagan.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {
    private Long orderId;
    private String orderReference;
    private String customerEmail;
    private BigDecimal totalAmount;
    private String orderStatus;
    private String paymentStatus;
    private String shippingAddress;
    private List<OrderItemResponseDto> items;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponseDto {
        private Long productId;
        private Integer quantity;
        private BigDecimal unitPrice;
    }
}