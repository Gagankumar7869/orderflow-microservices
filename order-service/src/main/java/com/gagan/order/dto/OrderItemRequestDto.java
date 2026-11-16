package com.gagan.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequestDto {

    @NotNull(message = "Product ID is mandatory.")
    private Long productId;

    @NotNull(message = "Quantity is mandatory.")
    @Min(value = 1, message = "Quantity must be at least 1.")
    private Integer quantity;
}