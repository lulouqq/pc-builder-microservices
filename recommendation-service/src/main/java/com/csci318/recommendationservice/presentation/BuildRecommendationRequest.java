package com.csci318.recommendationservice.presentation;

import com.csci318.recommendationservice.service.GenerateRecommendationCommand;

/**
 * R1 request body: budget and purpose are required, preferences is optional free text.
 */
public record BuildRecommendationRequest(String userId, Float budget, String purpose, String preferences) {

    public GenerateRecommendationCommand toCommand() {
        return new GenerateRecommendationCommand(userId, budget, purpose, preferences);
    }
}
