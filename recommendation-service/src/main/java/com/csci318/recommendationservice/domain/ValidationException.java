package com.csci318.recommendationservice.domain;

/**
 * Thrown when a domain invariant or request rule is violated (mapped to 400 ValidationFailed).
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
