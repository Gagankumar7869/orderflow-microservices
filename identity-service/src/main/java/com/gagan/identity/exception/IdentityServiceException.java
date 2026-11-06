package com.gagan.identity.exception;

import com.gagan.identity.constant.ErrorCodeEnum;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class IdentityServiceException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final String errorMessage;
    private final HttpStatus httpStatus;

    public IdentityServiceException(ErrorCodeEnum errorCodeEnum) {
        super(errorCodeEnum.getErrorMessage());
        this.errorCode = errorCodeEnum.getErrorCode();
        this.errorMessage = errorCodeEnum.getErrorMessage();
        this.httpStatus = errorCodeEnum.getHttpStatus();
    }

    public IdentityServiceException(ErrorCodeEnum errorCodeEnum, String customMessage) {
        super(customMessage);
        this.errorCode = errorCodeEnum.getErrorCode();
        this.errorMessage = customMessage;
        this.httpStatus = errorCodeEnum.getHttpStatus();
    }
}