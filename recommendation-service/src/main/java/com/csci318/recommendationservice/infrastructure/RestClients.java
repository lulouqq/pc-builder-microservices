package com.csci318.recommendationservice.infrastructure;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.web.client.RestClient;

import java.time.Duration;

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
}
