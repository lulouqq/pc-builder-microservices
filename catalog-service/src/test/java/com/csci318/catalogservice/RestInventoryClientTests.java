package com.csci318.catalogservice;

import com.csci318.catalogservice.domain.StockStatus;
import com.csci318.catalogservice.infrastructure.RestInventoryClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * C2: the REST adapter for Inventory Service, tested against a local stub HTTP server that answers
 * GET /api/inventory/{productId} with the provisional InventoryResponse contract
 * (quantity, reservedQuantity).
 */
class RestInventoryClientTests {

    private HttpServer inventoryService;
    private RestInventoryClient client;

    @BeforeEach
    void startStubInventoryService() throws IOException {
        inventoryService = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        inventoryService.start();
        client = new RestInventoryClient(RestClient.builder(),
                "http://localhost:" + inventoryService.getAddress().getPort() + "/api", Duration.ofMillis(500));
    }

    @AfterEach
    void stopStubInventoryService() {
        inventoryService.stop(0);
    }

    @Test
    void inStockWhenAvailableQuantityIsPositive() {
        stubInventory("p1", 200, "{\"productId\": \"p1\", \"quantity\": 10, \"reservedQuantity\": 3}");

        assertThat(client.getStockStatus("p1")).isEqualTo(StockStatus.IN_STOCK);
    }

    @Test
    void outOfStockWhenQuantityIsZero() {
        stubInventory("p1", 200, "{\"productId\": \"p1\", \"quantity\": 0, \"reservedQuantity\": 0}");

        assertThat(client.getStockStatus("p1")).isEqualTo(StockStatus.OUT_OF_STOCK);
    }

    @Test
    void outOfStockWhenAllStockIsReserved() {
        stubInventory("p1", 200, "{\"productId\": \"p1\", \"quantity\": 4, \"reservedQuantity\": 4}");

        assertThat(client.getStockStatus("p1")).isEqualTo(StockStatus.OUT_OF_STOCK);
    }

    @Test
    void unknownWhenInventoryHasNoRecordForTheProduct() {
        stubInventory("p1", 404, "{\"error\": \"ResourceNotFound\", \"message\": \"Inventory not found\"}");

        assertThat(client.getStockStatus("p1")).isEqualTo(StockStatus.UNKNOWN);
    }

    @Test
    void unknownWhenInventoryFails() {
        stubInventory("p1", 500, "{\"error\": \"InternalServiceError\"}");

        assertThat(client.getStockStatus("p1")).isEqualTo(StockStatus.UNKNOWN);
    }

    @Test
    void unknownWhenResponseDoesNotHaveTheExpectedFields() {
        stubInventory("p1", 200, "{\"productId\": \"p1\", \"availableQuantity\": 7}");
        stubInventory("p2", 200, "not json");

        assertThat(client.getStockStatus("p1")).isEqualTo(StockStatus.UNKNOWN);
        assertThat(client.getStockStatus("p2")).isEqualTo(StockStatus.UNKNOWN);
    }

    @Test
    void unknownWhenInventoryDoesNotRespondInTime() {
        inventoryService.createContext("/api/inventory/p1", exchange -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });

        assertThat(client.getStockStatus("p1")).isEqualTo(StockStatus.UNKNOWN);
    }

    @Test
    void unknownWhenInventoryIsDown() {
        inventoryService.stop(0);

        assertThat(client.getStockStatus("p1")).isEqualTo(StockStatus.UNKNOWN);
    }

    private void stubInventory(String productId, int status, String body) {
        inventoryService.createContext("/api/inventory/" + productId, exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
    }
}
