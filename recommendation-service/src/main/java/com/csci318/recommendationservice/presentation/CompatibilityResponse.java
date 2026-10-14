package com.csci318.recommendationservice.presentation;

import com.csci318.recommendationservice.domain.CompatibilityConflict;
import com.csci318.recommendationservice.domain.CompatibilityResult;

import java.util.List;

/**
 * R2 response: compatible is true when there are no conflicts.
 */
public record CompatibilityResponse(boolean compatible, List<ConflictResponse> conflicts) {

    public static CompatibilityResponse from(CompatibilityResult result) {
        return new CompatibilityResponse(result.compatible(),
                result.conflicts().stream().map(ConflictResponse::from).toList());
    }

    public record ConflictResponse(String specName, List<String> productIds, String message) {

        static ConflictResponse from(CompatibilityConflict conflict) {
            return new ConflictResponse(conflict.specName(), conflict.productIds(), conflict.message());
        }
    }
}
