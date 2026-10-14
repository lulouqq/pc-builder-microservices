package com.csci318.recommendationservice.service;

/**
 * Thrown when no valid PC build (compatible, in stock, within the budget) can be generated for an R1 request
 * (mapped to 409 CompatibilityConflict).
 */
public class NoValidBuildException extends RuntimeException {

    public NoValidBuildException(String message) {
        super(message);
    }
}
