package com.csci318.orderservice.service;

import java.util.List;

/**
 * The products of a recommended PC build, as read from Recommendation Service.
 */
public record RecommendedBuild(String recommendationId, String userId, List<String> productIds) {
}
