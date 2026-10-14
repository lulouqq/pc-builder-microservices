package com.csci318.recommendationservice.domain;

import java.util.List;

/**
 * Value object: one broken compatibility rule between two selected products.
 */
public record CompatibilityConflict(String specName, List<String> productIds, String message) {

    public CompatibilityConflict {
        productIds = List.copyOf(productIds);
    }
}
