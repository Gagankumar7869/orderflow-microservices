package com.gagan.order.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        String errorCode,
        String errorMessage,
        Map<String, String> fieldErrors,
        LocalDateTime timestamp
) {
    public static ErrorResponse of(String errorCode, String errorMessage) {
        return new ErrorResponse(errorCode, errorMessage, null, LocalDateTime.now());
    }

    public static ErrorResponse ofValidation(String errorCode, String errorMessage, Map<String, String> fieldErrors) {
        return new ErrorResponse(errorCode, errorMessage, fieldErrors, LocalDateTime.now());
    }
}