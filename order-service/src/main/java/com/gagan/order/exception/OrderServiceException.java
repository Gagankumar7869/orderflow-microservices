package com.gagan.order.exception;

import com.gagan.order.constant.OrderErrorCode;

import lombok.Getter;

@Getter
public class OrderServiceException extends RuntimeException {

    private final OrderErrorCode errorCode;

    public OrderServiceException(OrderErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public OrderServiceException(OrderErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}