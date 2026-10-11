package com.csci318.catalogservice.presentation.dto;

/**
 * Error body: the error code from the API error contract and a human-readable message.
 */
public record ErrorResponse(String error, String message) {
}
