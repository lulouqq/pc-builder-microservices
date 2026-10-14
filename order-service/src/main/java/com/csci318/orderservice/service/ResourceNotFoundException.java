package com.csci318.orderservice.service;

/**
 * The requested resource does not exist (ResourceNotFound, 404).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
