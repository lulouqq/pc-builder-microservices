package com.csci318.recommendationservice.infrastructure;

import com.csci318.recommendationservice.service.AccountClient;
import com.csci318.recommendationservice.service.AuthenticationRequiredException;
import com.csci318.recommendationservice.service.ResourceNotFoundException;
import com.csci318.recommendationservice.service.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Optional;

/**
 * Calls Account Service {@code GET /users/{userId}/preferences} (A3), forwarding the customer's
 * Authorization header. Response: {@code PreferenceResponse {userId, preference}}.
 * Account's 401, 403 and 404 are passed on as the same errors; a timeout, network failure or any other
 * error response is 503 ServiceUnavailable.
 */
@Component
public class RestAccountClient implements AccountClient {

    private static final Logger log = LoggerFactory.getLogger(RestAccountClient.class);

    private final RestClient restClient;

    public RestAccountClient(RestClient.Builder builder,
                             @Value("${account-service.base-url}") String baseUrl,
                             @Value("${account-service.timeout}") Duration timeout) {
        this.restClient = RestClients.create(builder, baseUrl, timeout);
    }

    @Override
    public Optional<String> getSavedPreference(String userId, String authorizationHeader) {
        try {
            PreferenceResponse response = restClient.get()
                    .uri("/users/{userId}/preferences", userId)
                    .headers(headers -> {
                        if (authorizationHeader != null) {
                            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
                        }
                    })
                    .retrieve()
                    .body(PreferenceResponse.class);
            return Optional.ofNullable(response)
                    .map(PreferenceResponse::preference)
                    .filter(preference -> !preference.isBlank());
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new AuthenticationRequiredException("Account Service did not accept the access token");
        } catch (HttpClientErrorException.Forbidden e) {
            throw new AccessDeniedException("Account Service denied access to the user's preferences");
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("User not found: " + userId);
        } catch (RestClientException e) {
            // connection failures, timeouts, 5xx and any other error response
            log.warn("Could not get the saved preference of user {}: {}", userId, e.getMessage());
            throw new ServiceUnavailableException("Account Service is unavailable", e);
        }
    }

    record PreferenceResponse(String userId, String preference) {
    }
}
