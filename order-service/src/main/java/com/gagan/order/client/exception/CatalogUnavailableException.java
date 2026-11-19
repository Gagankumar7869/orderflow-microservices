package com.gagan.order.client.exception;

import lombok.Getter;

@Getter
public class CatalogUnavailableException extends RuntimeException {

    private final boolean indeterminate;

    public CatalogUnavailableException(String message, boolean indeterminate) {
        super(message);
        this.indeterminate = indeterminate;
    }

    public CatalogUnavailableException(String message, boolean indeterminate, Throwable cause) {
        super(message, cause);
        this.indeterminate = indeterminate;
    }
}