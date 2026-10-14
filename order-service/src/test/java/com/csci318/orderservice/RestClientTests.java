package com.csci318.orderservice;

import com.csci318.orderservice.infrastructure.RestAccountClient;
import com.csci318.orderservice.infrastructure.RestCatalogClient;
import com.csci318.orderservice.infrastructure.RestRecommendationClient;
import com.csci318.orderservice.service.AuthenticationRequiredException;
import com.csci318.orderservice.service.CatalogProduct;
import com.csci318.orderservice.service.RecommendedBuild;
import com.csci318.orderservice.service.ResourceNotFoundException;
import com.csci318.orderservice.service.ServiceUnavailableException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The REST adapters for Catalog, Account and Recommendation Service, against a local stub HTTP
 * server that answers with those services' response shapes.
 */
class RestClientTests {

    private static final Duration TIMEOUT = Duration.ofMillis(500);

    private HttpServer server;
    private String baseUrl;
    private final AtomicReference<String> receivedAuthorization = new AtomicReference<>();

    @BeforeEach
    void startStubServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort() + "/api";
    }

    @AfterEach
    void stopStubServer() {
        server.stop(0);
    }

    // --- Catalog ---

    @Test
    void catalogClientFindsAProductAndItsPriceInTheProductList() {
        stub("/api/products", 200, """
                [{"id": "cpu", "name": "Ryzen 5", "price": 299.0, "brand": "AMD", "categoryId": "c1",
                  "specifications": [{"name": "Socket", "value": "AM5"}], "stockStatus": null},
                 {"id": "ram", "name": "16GB", "price": 80.5, "brand": "Corsair", "categoryId": "c2",
                  "specifications": [], "stockStatus": null}]
                """);
        RestCatalogClient client = new RestCatalogClient(RestClient.builder(), baseUrl, TIMEOUT);

        assertThat(client.findProduct("ram")).contains(new CatalogProduct("ram", "16GB", 80.5f));
        assertThat(client.findProduct("gpu")).isEmpty();
    }

    @Test
    void catalogClientReportsUnavailableCatalog() {
        stub("/api/products", 500, "{\"error\": \"InternalServiceError\"}");
        RestCatalogClient client = new RestCatalogClient(RestClient.builder(), baseUrl, TIMEOUT);

        assertThatThrownBy(() -> client.findProduct("cpu")).isInstanceOf(ServiceUnavailableException.class);

        server.stop(0);
        assertThatThrownBy(() -> client.findProduct("cpu")).isInstanceOf(ServiceUnavailableException.class);
    }

    // --- Account ---

    @Test
    void accountClientReadsSavedAddressIdsAndForwardsTheToken() {
        stub("/api/users/user-1/addresses", 200, """
                [{"id": "address-1", "streetNo": 1, "streetName": "Main St", "suburb": "A", "city": "B",
                  "state": "NSW", "postCode": 2500},
                 {"id": "address-2", "streetNo": 2, "streetName": "High St", "suburb": "A", "city": "B",
                  "state": "NSW", "postCode": 2500}]
                """);
        RestAccountClient client = new RestAccountClient(RestClient.builder(), baseUrl, TIMEOUT);

        assertThat(client.getSavedAddressIds("user-1", "Bearer customer-token"))
                .containsExactly("address-1", "address-2");
        assertThat(receivedAuthorization.get()).isEqualTo("Bearer customer-token");
    }

    @Test
    void accountClientPassesOnAccountErrors() {
        stub("/api/users/unauthorized/addresses", 401, "{\"code\": \"AuthenticationRequired\"}");
        stub("/api/users/forbidden/addresses", 403, "{\"code\": \"AccessDenied\"}");
        stub("/api/users/unknown/addresses", 404, "{\"code\": \"ResourceNotFound\"}");
        stub("/api/users/broken/addresses", 500, "{\"code\": \"InternalServiceError\"}");
        RestAccountClient client = new RestAccountClient(RestClient.builder(), baseUrl, TIMEOUT);

        assertThatThrownBy(() -> client.getSavedAddressIds("unauthorized", "Bearer t"))
                .isInstanceOf(AuthenticationRequiredException.class);
        assertThatThrownBy(() -> client.getSavedAddressIds("forbidden", "Bearer t"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> client.getSavedAddressIds("unknown", "Bearer t"))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> client.getSavedAddressIds("broken", "Bearer t"))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    // --- Recommendation ---

    @Test
    void recommendationClientReadsTheBuildComponentsAndForwardsTheToken() {
        stub("/api/recommendations/rec-1", 200, """
                {"recommendationId": "rec-1", "buildId": "build-1", "userId": "user-1", "budget": 1500.0,
                 "purpose": "gaming", "preferences": null, "totalPrice": 379.0, "description": "d",
                 "reasoning": "r",
                 "components": [{"productId": "cpu", "componentType": "CPU", "price": 299.0},
                                {"productId": "ram", "componentType": "RAM", "price": 80.0}]}
                """);
        RestRecommendationClient client = new RestRecommendationClient(RestClient.builder(), baseUrl, TIMEOUT);

        RecommendedBuild build = client.getRecommendedBuild("rec-1", "Bearer customer-token");

        assertThat(build.userId()).isEqualTo("user-1");
        assertThat(build.productIds()).containsExactly("cpu", "ram");
        assertThat(receivedAuthorization.get()).isEqualTo("Bearer customer-token");
    }

    @Test
    void recommendationClientPassesOnRecommendationErrors() {
        stub("/api/recommendations/unauthorized", 401, "{\"code\": \"AuthenticationRequired\"}");
        stub("/api/recommendations/forbidden", 403, "{\"code\": \"AccessDenied\"}");
        stub("/api/recommendations/unknown", 404, "{\"code\": \"ResourceNotFound\"}");
        stub("/api/recommendations/broken", 503, "{\"code\": \"ServiceUnavailable\"}");
        stub("/api/recommendations/empty", 200, "{}");
        RestRecommendationClient client = new RestRecommendationClient(RestClient.builder(), baseUrl, TIMEOUT);

        assertThatThrownBy(() -> client.getRecommendedBuild("unauthorized", "Bearer t"))
                .isInstanceOf(AuthenticationRequiredException.class);
        assertThatThrownBy(() -> client.getRecommendedBuild("forbidden", "Bearer t"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> client.getRecommendedBuild("unknown", "Bearer t"))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> client.getRecommendedBuild("broken", "Bearer t"))
                .isInstanceOf(ServiceUnavailableException.class);
        assertThatThrownBy(() -> client.getRecommendedBuild("empty", "Bearer t"))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    private void stub(String path, int status, String body) {
        server.createContext(path, exchange -> {
            receivedAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
    }
}
