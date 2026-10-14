package com.csci318.recommendationservice.presentation;

/**
 * Error body using the error codes from the API error response contract (e.g. ValidationFailed, ResourceNotFound).
 */
public record ErrorResponse(String code, String message) {
}
