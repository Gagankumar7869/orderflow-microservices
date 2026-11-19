package com.gagan.order.client.exception;

public class CatalogProductNotFoundException extends RuntimeException {
    public CatalogProductNotFoundException(String message) {
        super(message);
    }
}