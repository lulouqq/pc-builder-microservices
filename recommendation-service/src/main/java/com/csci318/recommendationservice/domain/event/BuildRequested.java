package com.csci318.recommendationservice.domain.event;

import com.csci318.recommendationservice.domain.BuildRequest;

import java.time.LocalDateTime;

/**
 * Domain event (internal to Recommendation Service, not published to Kafka): a customer's build request (R1)
 * was accepted and stored.
 */
public record BuildRequested(String buildRequestId, String userId, Float budget, String purpose,
                             LocalDateTime occurredAt) {

    public static BuildRequested of(BuildRequest request) {
        return new BuildRequested(request.getId(), request.getUserId(), request.getBudget(), request.getPurpose(),
                LocalDateTime.now());
    }
}
