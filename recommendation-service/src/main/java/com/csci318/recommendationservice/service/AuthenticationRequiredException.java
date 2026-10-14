package com.csci318.recommendationservice.service;

/**
 * Thrown when a downstream service rejects the customer's forwarded token as missing, invalid or expired
 * (mapped to 401 AuthenticationRequired).
 */
public class AuthenticationRequiredException extends RuntimeException {

    public AuthenticationRequiredException(String message) {
        super(message);
    }
}
