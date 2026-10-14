package com.csci318.recommendationservice.domain.event;

import com.csci318.recommendationservice.domain.BuildRequest;
import com.csci318.recommendationservice.domain.PCBuild;

import java.time.LocalDateTime;

/**
 * Domain event (internal to Recommendation Service, not published to Kafka): a valid PC build and its
 * recommendation were generated for a build request (R1).
 */
public record RecommendationGenerated(String recommendationId, String buildId, String buildRequestId,
                                      String userId, Float totalPrice, LocalDateTime occurredAt) {

    public static RecommendationGenerated of(BuildRequest request, PCBuild build) {
        return new RecommendationGenerated(build.getRecommendation().getId(), build.getId(), request.getId(),
                request.getUserId(), build.getTotalPrice(), LocalDateTime.now());
    }
}
