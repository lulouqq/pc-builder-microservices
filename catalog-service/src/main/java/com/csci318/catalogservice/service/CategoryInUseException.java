package com.csci318.catalogservice.service;

/**
 * The category still has products and cannot be removed (409).
 */
public class CategoryInUseException extends RuntimeException {

    public CategoryInUseException(String message) {
        super(message);
    }
}
