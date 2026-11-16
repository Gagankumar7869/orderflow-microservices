
package com.gagan.order.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderServiceException.class)
    public ResponseEntity<Map<String, Object>> handleOrderServiceException(
            OrderServiceException ex) {

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("errorCode", ex.getErrorCode());
        errorResponse.put("errorMessage", ex.getErrorMessage());

        HttpStatus status = HttpStatus.BAD_REQUEST;
        String code = ex.getErrorCode();

        if ("30001".equals(code)) {
            status = HttpStatus.NOT_FOUND;
        } else if ("30002".equals(code)
                || "30020".equals(code)
                || "30021".equals(code)) {
            status = HttpStatus.CONFLICT;
        } else if ("30003".equals(code)) {
            status = HttpStatus.FORBIDDEN;
        } else if ("30010".equals(code)
                || "30015".equals(code)
                || "30030".equals(code)) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
        }

        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> validationErrors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            validationErrors.put(
                    error.getField(),
                    error.getDefaultMessage()
            );
        }

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("errorCode", "30004");
        errorResponse.put("errorMessage",
                "Validation failed for request parameters.");
        errorResponse.put("validationErrors", validationErrors);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(
            Exception ex) {

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("errorCode", "30099");
        errorResponse.put("errorMessage",
                "An unexpected error occurred: "
                + (ex.getMessage() != null ? ex.getMessage() : "Unknown"));

        return new ResponseEntity<>(
                errorResponse,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
