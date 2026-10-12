package com.csci318.accountservice.service;

/**
 * Thrown when a requested resource does not exist (mapped to 404 ResourceNotFound).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
