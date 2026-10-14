package com.csci318.recommendationservice.infrastructure;

import com.csci318.recommendationservice.service.InventoryClient;
import com.csci318.recommendationservice.service.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * Calls Inventory Service {@code GET /inventory/{productId}} (public).
 *
 * Contract (specs/api-endpoints.md, Inventory Service DTOs): {@code InventoryResponse {productId, quantity,
 * reservedQuantity}}; available stock is {@code quantity - reservedQuantity}. A 404 (no inventory record) means
 * the product is not available.
 */
@Component
public class RestInventoryClient implements InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(RestInventoryClient.class);

    private final RestClient restClient;

    public RestInventoryClient(RestClient.Builder builder,
                               @Value("${inventory-service.base-url}") String baseUrl,
                               @Value("${inventory-service.timeout}") Duration timeout) {
        this.restClient = RestClients.create(builder, baseUrl, timeout);
    }

    @Override
    public boolean isInStock(String productId) {
        InventoryResponse response;
        try {
            response = restClient.get()
                    .uri("/inventory/{productId}", productId)
                    .retrieve()
                    .body(InventoryResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            // no inventory record: the product is not available
            return false;
        } catch (RestClientException e) {
            log.warn("Could not get stock for product {}: {}", productId, e.getMessage());
            throw new ServiceUnavailableException("Inventory Service is unavailable", e);
        }
        if (response == null || response.quantity() == null || response.reservedQuantity() == null) {
            log.warn("Inventory Service returned no usable stock information for product {}", productId);
            throw new ServiceUnavailableException("Inventory Service returned no usable stock information");
        }
        return response.quantity() - response.reservedQuantity() > 0;
    }

    /** Inventory Service's InventoryResponse. */
    record InventoryResponse(String productId, Integer quantity, Integer reservedQuantity) {
    }
}
