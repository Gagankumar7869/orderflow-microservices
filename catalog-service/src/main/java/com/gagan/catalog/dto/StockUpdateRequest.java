package com.gagan.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockUpdateRequest {

    @NotBlank(message = "Order reference is required for stock operations")
    private String orderReference;

    @NotNull(message = "Stock adjustment quantity is required")
    @Min(value = 1, message = "Stock adjustment quantity must be at least 1")
    private Integer quantity;
}