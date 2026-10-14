package com.csci318.recommendationservice.domain.event;

import com.csci318.recommendationservice.domain.CompatibilityResult;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Domain event (internal to Recommendation Service, not published to Kafka): a set of components was
 * checked for compatibility (R2).
 */
public record CompatibilityChecked(List<String> productIds, boolean compatible, int conflictCount,
                                   LocalDateTime occurredAt) {

    public static CompatibilityChecked of(List<String> productIds, CompatibilityResult result) {
        return new CompatibilityChecked(List.copyOf(productIds), result.compatible(), result.conflicts().size(),
                LocalDateTime.now());
    }
}
