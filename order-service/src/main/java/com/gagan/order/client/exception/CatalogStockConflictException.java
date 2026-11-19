package com.gagan.order.client.exception;

public class CatalogStockConflictException extends RuntimeException {
    public CatalogStockConflictException(String message) {
        super(message);
    }
}