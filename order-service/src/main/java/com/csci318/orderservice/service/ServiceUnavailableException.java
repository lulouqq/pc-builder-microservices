package com.csci318.orderservice.service;

/**
 * A required downstream service is unavailable or timed out (ServiceUnavailable, 503).
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
