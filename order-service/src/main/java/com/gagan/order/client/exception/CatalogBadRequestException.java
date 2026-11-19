package com.gagan.order.client.exception;

public class CatalogBadRequestException extends RuntimeException {
    public CatalogBadRequestException(String message) {
        super(message);
    }
}