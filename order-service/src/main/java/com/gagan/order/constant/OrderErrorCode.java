package com.gagan.order.constant;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum OrderErrorCode {
    VALIDATION_ERROR("ORD-400-VALIDATION", HttpStatus.BAD_REQUEST, "Request validation failed"),
    MISSING_IDEMPOTENCY_KEY("ORD-400-IDEMPOTENCY-KEY", HttpStatus.BAD_REQUEST, "Idempotency-Key header is required"),
    ORDER_NOT_FOUND("ORD-404-NOT-FOUND", HttpStatus.NOT_FOUND, "Order not found"),
    ROUTE_NOT_FOUND("ORD-404-ROUTE", HttpStatus.NOT_FOUND, "Requested endpoint does not exist"),
    FORBIDDEN_ORDER_ACCESS("ORD-403-FORBIDDEN", HttpStatus.FORBIDDEN, "You do not have permission to access this order"),
    IDEMPOTENCY_CONFLICT("ORD-409-IDEMPOTENCY", HttpStatus.CONFLICT, "Idempotency key reused with a different request payload"),
    CHECKOUT_IN_PROGRESS("ORD-409-IN-PROGRESS", HttpStatus.CONFLICT, "A checkout with this idempotency key is currently processing"),
    INVALID_ORDER_STATE("ORD-409-INVALID-STATE", HttpStatus.CONFLICT, "Order cannot transition from its current state"),
    INSUFFICIENT_STOCK("ORD-409-STOCK", HttpStatus.CONFLICT, "Insufficient stock available for one or more items"),
    CATALOG_SERVICE_UNAVAILABLE("ORD-503-CATALOG", HttpStatus.SERVICE_UNAVAILABLE, "Catalog service is currently unavailable"),
    INTERNAL_ERROR("ORD-500-INTERNAL", HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal error occurred");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    OrderErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }
}