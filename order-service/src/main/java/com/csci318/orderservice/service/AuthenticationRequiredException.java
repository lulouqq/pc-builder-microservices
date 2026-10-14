package com.csci318.orderservice.service;

/**
 * A downstream service rejected the forwarded access token (AuthenticationRequired, 401).
 */
public class AuthenticationRequiredException extends RuntimeException {

    public AuthenticationRequiredException(String message) {
        super(message);
    }
}
