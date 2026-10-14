package com.csci318.recommendationservice.domain;

import java.util.List;

/**
 * Value object: the outcome of a compatibility check; compatible when there are no conflicts.
 */
public record CompatibilityResult(boolean compatible, List<CompatibilityConflict> conflicts) {

    public static CompatibilityResult of(List<CompatibilityConflict> conflicts) {
        return new CompatibilityResult(conflicts.isEmpty(), List.copyOf(conflicts));
    }
}
