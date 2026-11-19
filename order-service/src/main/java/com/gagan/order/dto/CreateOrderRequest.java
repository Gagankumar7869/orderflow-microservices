package com.gagan.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank(message = "customerEmail is required")
        @Email(message = "customerEmail must be a valid email address")
        String customerEmail,

        @NotBlank(message = "shippingAddress is required")
        @Size(max = 500, message = "shippingAddress must not exceed 500 characters")
        String shippingAddress,

        @NotEmpty(message = "items must contain at least one item")
        @Valid
        List<OrderItemRequest> items
) {}