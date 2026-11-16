package com.gagan.order.exception;

import com.gagan.order.constant.OrderErrorCodeEnum;
import lombok.Getter;

@Getter
public class OrderServiceException extends RuntimeException {
    private final String errorCode;
    private final String errorMessage;

    public OrderServiceException(OrderErrorCodeEnum errorEnum) {
        super(errorEnum.getErrorMessage());
        this.errorCode = errorEnum.getErrorCode();
        this.errorMessage = errorEnum.getErrorMessage();
    }
}