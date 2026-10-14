package com.csci318.recommendationservice.infrastructure;

import com.csci318.recommendationservice.service.ServiceUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Inventory Service GET /inventory/{productId}, with the provisional InventoryResponse (quantity, reservedQuantity).
 */
class RestInventoryClientTest {

    private StubHttpServer inventoryService;
    private RestInventoryClient client;

    @BeforeEach
    void start() throws IOException {
        inventoryService = new StubHttpServer();
        client = new RestInventoryClient(RestClient.builder(), inventoryService.baseUrl(), Duration.ofMillis(500));
    }

    @AfterEach
    void stop() {
        inventoryService.close();
    }

    @Test
    void inStockWhenAvailableQuantityIsPositive() {
        inventoryService.stub("/inventory/p1", 200, "{\"productId\": \"p1\", \"quantity\": 10, \"reservedQuantity\": 3}");

        assertThat(client.isInStock("p1")).isTrue();
    }

    @Test
    void notInStockWhenAllStockIsReserved() {
        inventoryService.stub("/inventory/p1", 200, "{\"productId\": \"p1\", \"quantity\": 4, \"reservedQuantity\": 4}");

        assertThat(client.isInStock("p1")).isFalse();
    }

    @Test
    void notInStockWhenInventoryHasNoRecord() {
        inventoryService.stub("/inventory/p1", 404, "{\"code\": \"ResourceNotFound\"}");

        assertThat(client.isInStock("p1")).isFalse();
    }

    @Test
    void unavailableWhenInventoryFails() {
        inventoryService.stub("/inventory/p1", 500, "{\"code\": \"InternalServiceError\"}");

        assertThatThrownBy(() -> client.isInStock("p1")).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void unavailableWhenResponseHasNoStockFields() {
        inventoryService.stub("/inventory/p1", 200, "{\"productId\": \"p1\"}");

        assertThatThrownBy(() -> client.isInStock("p1")).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void unavailableWhenInventoryIsDown() throws IOException {
        RestInventoryClient down = new RestInventoryClient(RestClient.builder(),
                StubHttpServer.unreachableBaseUrl(), Duration.ofMillis(500));

        assertThatThrownBy(() -> down.isInStock("p1")).isInstanceOf(ServiceUnavailableException.class);
    }
}
