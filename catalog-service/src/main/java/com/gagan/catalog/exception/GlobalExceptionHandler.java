package com.gagan.catalog.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.gagan.catalog.constant.ErrorCodeEnum;
import com.gagan.catalog.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final Map<String, ErrorCodeEnum> VALIDATION_ERROR_MAP = new HashMap<>();

    static {
        VALIDATION_ERROR_MAP.put("name:NotBlank", ErrorCodeEnum.CATEGORY_NAME_BLANK);
        VALIDATION_ERROR_MAP.put("name:Size", ErrorCodeEnum.CATEGORY_NAME_INVALID_LENGTH);
        VALIDATION_ERROR_MAP.put("sku:NotBlank", ErrorCodeEnum.SKU_BLANK);
        VALIDATION_ERROR_MAP.put("sku:Size", ErrorCodeEnum.SKU_INVALID_LENGTH);
        VALIDATION_ERROR_MAP.put("productName:NotBlank", ErrorCodeEnum.PRODUCT_NAME_BLANK);
        VALIDATION_ERROR_MAP.put("productName:Size", ErrorCodeEnum.PRODUCT_NAME_INVALID_LENGTH);
        VALIDATION_ERROR_MAP.put("price:NotNull", ErrorCodeEnum.PRICE_NULL);
        VALIDATION_ERROR_MAP.put("price:DecimalMin", ErrorCodeEnum.PRICE_INVALID);
        VALIDATION_ERROR_MAP.put("stockQuantity:NotNull", ErrorCodeEnum.STOCK_NULL);
        VALIDATION_ERROR_MAP.put("stockQuantity:Min", ErrorCodeEnum.STOCK_NEGATIVE);
        VALIDATION_ERROR_MAP.put("categoryId:NotNull", ErrorCodeEnum.CATEGORY_ID_NULL);
        VALIDATION_ERROR_MAP.put("quantity:NotNull", ErrorCodeEnum.QUANTITY_NULL);
        VALIDATION_ERROR_MAP.put("quantity:Min", ErrorCodeEnum.QUANTITY_INVALID);
        VALIDATION_ERROR_MAP.put("orderReference:NotBlank", ErrorCodeEnum.ORDER_REFERENCE_BLANK);
    }

    @ExceptionHandler(CatalogServiceException.class)
    public ResponseEntity<ErrorResponse> handleCatalogServiceException(
            CatalogServiceException ex,
            HttpServletRequest request) {

        log.warn("Catalog exception on path [{}]: code=[{}], message=[{}]",
                request.getRequestURI(), ex.getErrorCode(), ex.getErrorMessage());

        ErrorResponse error = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .errorMessage(ex.getErrorMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ErrorCodeEnum primaryError = ErrorCodeEnum.GENERIC_ERROR;

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());

            if (primaryError == ErrorCodeEnum.GENERIC_ERROR) {
                String lookupKey = fieldError.getField() + ":" + fieldError.getCode();
                primaryError = VALIDATION_ERROR_MAP.getOrDefault(lookupKey, ErrorCodeEnum.GENERIC_ERROR);
            }
        }

        log.warn("Validation failed on path [{}]: code=[{}], errors={}",
                request.getRequestURI(), primaryError.getErrorCode(), fieldErrors);

        ErrorResponse error = ErrorResponse.builder()
                .errorCode(primaryError.getErrorCode())
                .errorMessage(primaryError.getErrorMessage())
                .timestamp(LocalDateTime.now())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(primaryError.getHttpStatus()).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {

        ErrorCodeEnum errorEnum = ErrorCodeEnum.ACCESS_DENIED;
        log.warn("Method access denied on path [{}]: code=[{}]",
                request.getRequestURI(), errorEnum.getErrorCode());

        ErrorResponse error = ErrorResponse.builder()
                .errorCode(errorEnum.getErrorCode())
                .errorMessage(errorEnum.getErrorMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(errorEnum.getHttpStatus()).body(error);
    }
    
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            org.springframework.dao.DataIntegrityViolationException ex) {
        String rootMsg = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : "";

        if (rootMsg.contains("uk_reservation_order_product")) {
            ErrorCodeEnum errorEnum = ErrorCodeEnum.CONCURRENT_RESERVATION_CONFLICT;
            log.warn("Duplicate reservation constraint violation: {}", rootMsg);
            return ResponseEntity.status(errorEnum.getHttpStatus()).body(
                    ErrorResponse.builder()
                            .errorCode(errorEnum.getErrorCode())
                            .errorMessage(errorEnum.getErrorMessage())
                            .timestamp(LocalDateTime.now())
                            .build()
            );
        }

        log.error("Unhandled database integrity violation: {}", rootMsg, ex);
        ErrorCodeEnum genericError = ErrorCodeEnum.GENERIC_ERROR;
        return ResponseEntity.status(genericError.getHttpStatus()).body(
                ErrorResponse.builder()
                        .errorCode(genericError.getErrorCode())
                        .errorMessage(genericError.getErrorMessage())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        ErrorCodeEnum errorEnum = ErrorCodeEnum.GENERIC_ERROR;
        log.error("Unhandled exception on path [{}]: code=[{}]",
                request.getRequestURI(), errorEnum.getErrorCode(), ex);

        ErrorResponse error = ErrorResponse.builder()
                .errorCode(errorEnum.getErrorCode())
                .errorMessage(errorEnum.getErrorMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(errorEnum.getHttpStatus()).body(error);
    }
}