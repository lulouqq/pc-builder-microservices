package com.csci318.recommendationservice.service;

/**
 * Input of R1: the build request from the customer.
 */
public record GenerateRecommendationCommand(String userId, Float budget, String purpose, String preferences) {
}
