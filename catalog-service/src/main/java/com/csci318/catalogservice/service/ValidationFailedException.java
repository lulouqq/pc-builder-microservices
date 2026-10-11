package com.csci318.catalogservice.service;

/**
 * The request is invalid (ValidationFailed, 400).
 */
public class ValidationFailedException extends RuntimeException {

    public ValidationFailedException(String message) {
        super(message);
    }
}
