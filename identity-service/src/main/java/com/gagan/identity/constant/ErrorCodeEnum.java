package com.gagan.identity.constant;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCodeEnum {

    GENERIC_ERROR("10000", "An unexpected internal server error occurred.", HttpStatus.INTERNAL_SERVER_ERROR),

    
    FULL_NAME_BLANK("10001", "Full name must not be blank", HttpStatus.BAD_REQUEST),
    FULL_NAME_INVALID_LENGTH("10002", "Full name must be between 2 and 100 characters", HttpStatus.BAD_REQUEST),

    EMAIL_BLANK("10003", "Email must not be blank", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID("10004", "Email must be a valid format", HttpStatus.BAD_REQUEST),

    PASSWORD_BLANK("10005", "Password must not be blank", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID_LENGTH("10006", "Password must be between 8 and 64 characters", HttpStatus.BAD_REQUEST),

    
    EMAIL_ALREADY_EXISTS("10007", "A user with this email address is already registered.", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS("10008", "Invalid email or password.", HttpStatus.UNAUTHORIZED),
    USER_NOT_FOUND("10009", "Requested user account was not found.", HttpStatus.NOT_FOUND);

    private final String errorCode;
    private final String errorMessage;
    private final HttpStatus httpStatus;

    ErrorCodeEnum(String errorCode, String errorMessage, HttpStatus httpStatus) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.httpStatus = httpStatus;
    }
}