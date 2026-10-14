package com.csci318.recommendationservice.presentation;

import com.csci318.recommendationservice.domain.BuildRequest;
import com.csci318.recommendationservice.domain.PCBuild;
import com.csci318.recommendationservice.service.RecommendationView;

import java.util.List;

/**
 * R1/R3/R4 response: the BuildRequest (userId, budget, purpose, preferences), the PCBuild (buildId, totalPrice,
 * description, components) and the Recommendation (recommendationId, reasoning).
 */
public record RecommendationResponse(String recommendationId, String buildId, String userId, Float budget,
                                     String purpose, String preferences, Float totalPrice, String description,
                                     String reasoning, List<ComponentResponse> components) {

    public static RecommendationResponse from(RecommendationView view) {
        BuildRequest request = view.request();
        PCBuild build = view.build();
        return new RecommendationResponse(build.getRecommendation().getId(), build.getId(), request.getUserId(),
                request.getBudget(), request.getPurpose(), request.getPreferences(), build.getTotalPrice(),
                build.getDescription(), build.getRecommendation().getReasoning(),
                build.getComponents().stream().map(ComponentResponse::from).toList());
    }
}
