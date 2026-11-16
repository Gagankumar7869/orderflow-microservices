package com.gagan.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequestDto {

    @NotNull(message = "Order reference is mandatory.")
    private String orderReference;

    @NotNull(message = "Customer email is mandatory.")
    private String customerEmail;

    private String shippingAddress;

    @NotEmpty(message = "Order must contain at least one item.")
    @Valid
    private List<OrderItemRequestDto> items;
}