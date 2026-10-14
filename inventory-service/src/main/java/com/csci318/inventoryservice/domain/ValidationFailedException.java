package com.csci318.inventoryservice.domain;

/**
 * Invalid command or query input (ValidationFailed, 400).
 */
public class ValidationFailedException extends RuntimeException {

    public ValidationFailedException(String message) {
        super(message);
    }
}
