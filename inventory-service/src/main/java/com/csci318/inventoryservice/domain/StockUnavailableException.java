package com.csci318.inventoryservice.domain;

/**
 * Requested stock cannot be reserved (StockUnavailable, 409).
 */
public class StockUnavailableException extends RuntimeException {

    public StockUnavailableException(String message) {
        super(message);
    }
}
