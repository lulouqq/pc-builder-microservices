package com.csci318.recommendationservice.service;

import com.csci318.recommendationservice.domain.BuildRequest;
import com.csci318.recommendationservice.domain.PCBuild;

/**
 * A build request together with the PC build (and its recommendation) generated for it.
 */
public record RecommendationView(BuildRequest request, PCBuild build) {
}
