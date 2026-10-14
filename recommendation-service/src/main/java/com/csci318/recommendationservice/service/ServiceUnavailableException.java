package com.csci318.recommendationservice.service;

/**
 * Thrown when Account, Catalog or Inventory Service, or the LLM, is unavailable, times out or fails
 * (mapped to 503 ServiceUnavailable).
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
