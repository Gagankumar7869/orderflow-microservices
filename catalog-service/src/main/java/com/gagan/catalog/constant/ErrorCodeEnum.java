package com.gagan.catalog.constant;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCodeEnum {

    GENERIC_ERROR("20000", "An unexpected internal server error occurred in catalog-service.", HttpStatus.INTERNAL_SERVER_ERROR),
    CATEGORY_NAME_BLANK("20001", "Category name must not be blank", HttpStatus.BAD_REQUEST),
    CATEGORY_NAME_INVALID_LENGTH("20002", "Category name must be between 2 and 100 characters", HttpStatus.BAD_REQUEST),
    SKU_BLANK("20003", "Product SKU must not be blank", HttpStatus.BAD_REQUEST),
    SKU_INVALID_LENGTH("20004", "Product SKU must be between 3 and 64 characters", HttpStatus.BAD_REQUEST),
    PRODUCT_NAME_BLANK("20005", "Product name must not be blank", HttpStatus.BAD_REQUEST),
    PRODUCT_NAME_INVALID_LENGTH("20006", "Product name must be between 2 and 150 characters", HttpStatus.BAD_REQUEST),
    PRICE_NULL("20007", "Product price is required", HttpStatus.BAD_REQUEST),
    PRICE_INVALID("20008", "Product price must be greater than zero", HttpStatus.BAD_REQUEST),
    STOCK_NULL("20009", "Stock quantity is required", HttpStatus.BAD_REQUEST),
    STOCK_NEGATIVE("20010", "Stock quantity cannot be negative", HttpStatus.BAD_REQUEST),
    CATEGORY_ID_NULL("20011", "Category ID is required", HttpStatus.BAD_REQUEST),
    CATEGORY_ALREADY_EXISTS("20012", "A category with this name already exists.", HttpStatus.CONFLICT),
    CATEGORY_NOT_FOUND("20013", "Requested category was not found.", HttpStatus.NOT_FOUND),
    SKU_ALREADY_EXISTS("20014", "A product with this SKU already exists.", HttpStatus.CONFLICT),
    PRODUCT_NOT_FOUND("20015", "Requested product was not found.", HttpStatus.NOT_FOUND),
    INSUFFICIENT_STOCK("20016", "Insufficient stock available for the requested product.", HttpStatus.CONFLICT),
    INVALID_TOKEN("20017", "Authentication token is missing, expired, or invalid.", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("20018", "You do not have permission to perform this catalog operation.", HttpStatus.FORBIDDEN),
    QUANTITY_NULL("20019", "Stock adjustment quantity is required", HttpStatus.BAD_REQUEST),
    QUANTITY_INVALID("20020", "Stock adjustment quantity must be at least 1", HttpStatus.BAD_REQUEST);

    private final String errorCode;
    private final String errorMessage;
    private final HttpStatus httpStatus;

    ErrorCodeEnum(String errorCode, String errorMessage, HttpStatus httpStatus) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.httpStatus = httpStatus;
    }
}