package com.csci318.orderservice.domain;

/**
 * The request is invalid (ValidationFailed, 400).
 */
public class ValidationFailedException extends RuntimeException {

    public ValidationFailedException(String message) {
        super(message);
    }
}
