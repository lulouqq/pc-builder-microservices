package com.csci318.orderservice.infrastructure;

import com.csci318.orderservice.service.AuthenticationRequiredException;
import com.csci318.orderservice.service.RecommendationClient;
import com.csci318.orderservice.service.RecommendedBuild;
import com.csci318.orderservice.service.ResourceNotFoundException;
import com.csci318.orderservice.service.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Calls Recommendation Service {@code GET /recommendations/{recommendationId}} (R3), forwarding the
 * customer's Authorization header. Its 401, 403 and 404 are passed on as the same errors; a timeout,
 * network failure or any other error response is 503 ServiceUnavailable.
 */
@Component
public class RestRecommendationClient implements RecommendationClient {

    private static final Logger log = LoggerFactory.getLogger(RestRecommendationClient.class);

    private final RestClient restClient;

    public RestRecommendationClient(RestClient.Builder builder,
                                    @Value("${recommendation-service.base-url}") String baseUrl,
                                    @Value("${recommendation-service.timeout}") Duration timeout) {
        this.restClient = RestClients.create(builder, baseUrl, timeout);
    }

    @Override
    public RecommendedBuild getRecommendedBuild(String recommendationId, String authorizationHeader) {
        try {
            RecommendationResponse response = restClient.get()
                    .uri("/recommendations/{recommendationId}", recommendationId)
                    .headers(RestClients.forward(authorizationHeader))
                    .retrieve()
                    .body(RecommendationResponse.class);
            if (response == null || response.components() == null) {
                throw new ServiceUnavailableException("Recommendation Service returned no build components", null);
            }
            return new RecommendedBuild(recommendationId, response.userId(), response.components().stream()
                    .map(ComponentResponse::productId)
                    .filter(Objects::nonNull)
                    .toList());
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new AuthenticationRequiredException("Recommendation Service did not accept the access token");
        } catch (HttpClientErrorException.Forbidden e) {
            throw new AccessDeniedException("The recommendation belongs to another user");
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Recommendation not found: " + recommendationId);
        } catch (RestClientException e) {
            log.warn("Could not get recommendation {}: {}", recommendationId, e.getMessage());
            throw new ServiceUnavailableException("Recommendation Service is unavailable", e);
        }
    }

    /** Recommendation's RecommendationResponse; only these fields are used. */
    record RecommendationResponse(String userId, List<ComponentResponse> components) {
    }

    record ComponentResponse(String productId) {
    }
}
