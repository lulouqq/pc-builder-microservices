package com.csci318.accountservice.service;

/**
 * Login credentials are wrong (unknown username or wrong password); mapped to AuthenticationRequired (401).
 */
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
