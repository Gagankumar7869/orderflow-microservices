package com.gagan.order.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderErrorCodeEnum {
    // 3000x: Order Resource & Validation Errors
    ORDER_NOT_FOUND("30001", "Order not found with the provided reference or ID."),
    ORDER_ALREADY_EXISTS("30002", "An order with this reference already exists."),
    UNAUTHORIZED_ORDER_ACCESS("30003", "You do not have permission to access or modify this order."),
    INVALID_ORDER_ITEMS("30004", "Order must contain at least one valid item with a positive quantity."),

    // 3001x: Catalog & Stock Coordination Errors
    INSUFFICIENT_STOCK_FOR_CHECKOUT("30010", "Checkout failed because one or more items have insufficient stock."),
    CATALOG_SERVICE_UNAVAILABLE("30015", "Catalog service is currently unreachable. Please try again later."),

    // 3002x: Order State & Lifecycle Errors
    INVALID_ORDER_STATE("30020", "Order state transition is invalid for the requested operation."),
    ORDER_ALREADY_CANCELLED("30021", "This order has already been cancelled and cannot be modified."),

    // 3003x: Payment & Gateway Errors
    PAYMENT_PROCESSING_FAILED("30030", "Payment processing failed. Order has been marked as payment failed.");

    private final String errorCode;
    private final String errorMessage;
}