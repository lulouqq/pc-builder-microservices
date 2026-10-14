package com.csci318.orderservice.infrastructure;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Builds the RestClients for the downstream services, with the same connect and read timeout.
 */
final class RestClients {

    private RestClients() {
    }

    static RestClient create(RestClient.Builder builder, String baseUrl, Duration timeout) {
        return builder
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(timeout)
                        .withReadTimeout(timeout)))
                .build();
    }

    /** Forwards the customer's Authorization header to a protected endpoint. */
    static Consumer<HttpHeaders> forward(String authorizationHeader) {
        return headers -> {
            if (authorizationHeader != null) {
                headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
            }
        };
    }
}
