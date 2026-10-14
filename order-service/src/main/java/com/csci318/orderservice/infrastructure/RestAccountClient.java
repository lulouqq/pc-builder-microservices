package com.csci318.orderservice.infrastructure;

import com.csci318.orderservice.service.AccountClient;
import com.csci318.orderservice.service.AuthenticationRequiredException;
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Calls Account Service {@code GET /users/{userId}/addresses} (A1), forwarding the customer's
 * Authorization header. Account's 401, 403 and 404 are passed on as the same errors; a timeout,
 * network failure or any other error response is 503 ServiceUnavailable.
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
    public List<String> getSavedAddressIds(String userId, String authorizationHeader) {
        try {
            AddressResponse[] addresses = restClient.get()
                    .uri("/users/{userId}/addresses", userId)
                    .headers(RestClients.forward(authorizationHeader))
                    .retrieve()
                    .body(AddressResponse[].class);
            return addresses == null ? List.of() : Arrays.stream(addresses)
                    .map(AddressResponse::id)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new AuthenticationRequiredException("Account Service did not accept the access token");
        } catch (HttpClientErrorException.Forbidden e) {
            throw new AccessDeniedException("Account Service denied access to the user's addresses");
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("User not found: " + userId);
        } catch (RestClientException e) {
            log.warn("Could not get the saved addresses of user {}: {}", userId, e.getMessage());
            throw new ServiceUnavailableException("Account Service is unavailable", e);
        }
    }

    /** Account's AddressResponse; only the id is used. */
    record AddressResponse(String id) {
    }
}
