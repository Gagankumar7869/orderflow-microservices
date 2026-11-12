package com.gagan.catalog.exception;

import com.gagan.catalog.constant.ErrorCodeEnum;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CatalogServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final String errorMessage;
    private final HttpStatus httpStatus;

    public CatalogServiceException(ErrorCodeEnum errorCodeEnum) {
        super(errorCodeEnum.getErrorMessage());
        this.errorCode = errorCodeEnum.getErrorCode();
        this.errorMessage = errorCodeEnum.getErrorMessage();
        this.httpStatus = errorCodeEnum.getHttpStatus();
    }
}