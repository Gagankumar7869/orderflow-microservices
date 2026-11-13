package com.gagan.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleaseStockRequest {

    @NotBlank(message = "Order reference is required for stock operations")
    private String orderReference;
}