package com.csci318.recommendationservice.presentation;

import com.csci318.recommendationservice.service.RecommendationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /** R1: generate a recommended PC build for the authenticated customer. */
    @PostMapping("/recommendations")
    @ResponseStatus(HttpStatus.CREATED)
    public RecommendationResponse generate(@RequestBody BuildRecommendationRequest request,
                                           @AuthenticationPrincipal Jwt jwt,
                                           @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return RecommendationResponse.from(
                recommendationService.generate(request.toCommand(), jwt.getSubject(), authorization));
    }

    /** R3: view a recommendation and its components (also used by Order Service). */
    @GetMapping("/recommendations/{recommendationId}")
    public RecommendationResponse getRecommendation(@PathVariable String recommendationId,
                                                    @AuthenticationPrincipal Jwt jwt) {
        return RecommendationResponse.from(recommendationService.getRecommendation(recommendationId, jwt.getSubject()));
    }

    /** R4: the customer's previous build requests and recommendations (no guaranteed order). */
    @GetMapping("/users/{userId}/recommendations")
    public List<RecommendationResponse> getUserRecommendations(@PathVariable String userId) {
        return recommendationService.getUserRecommendations(userId).stream()
                .map(RecommendationResponse::from)
                .toList();
    }
}
