package com.csci318.orderservice.service;

/**
 * Inter-service query to Recommendation Service for the components of a recommended build (R3).
 */
public interface RecommendationClient {

    /**
     * The customer's Authorization header is forwarded; Recommendation Service checks the owner.
     *
     * @throws ResourceNotFoundException   when the recommendation does not exist
     * @throws ServiceUnavailableException when Recommendation Service cannot be reached
     */
    RecommendedBuild getRecommendedBuild(String recommendationId, String authorizationHeader);
}
