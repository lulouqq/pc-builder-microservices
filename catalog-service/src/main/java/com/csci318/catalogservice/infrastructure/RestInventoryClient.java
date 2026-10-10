package com.csci318.catalogservice.infrastructure;

import com.csci318.catalogservice.domain.StockStatus;
import com.csci318.catalogservice.service.InventoryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * Calls Inventory Service {@code GET /inventory/{productId}} over REST.
 *
 * PROVISIONAL CONTRACT: the specs do not define the fields of InventoryResponse. This class assumes
 * {@code quantity} and {@code reservedQuantity} (the InventoryItem fields in the domain model) and
 * must be confirmed against the Inventory Service implementation. The assumption is kept in this
 * class only.
 */
@Component
public class RestInventoryClient implements InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(RestInventoryClient.class);

    private final RestClient restClient;

    public RestInventoryClient(RestClient.Builder builder,
                               @Value("${inventory-service.base-url}") String baseUrl,
                               @Value("${inventory-service.timeout}") Duration timeout) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(timeout)
                        .withReadTimeout(timeout)))
                .build();
    }

    @Override
    public StockStatus getStockStatus(String productId) {
        try {
            InventoryResponse response = restClient.get()
                    .uri("/inventory/{productId}", productId)
                    .retrieve()
                    .body(InventoryResponse.class);
            if (response == null || response.quantity() == null || response.reservedQuantity() == null) {
                log.warn("Inventory Service returned no usable stock information for product {}", productId);
                return StockStatus.UNKNOWN;
            }
            return StockStatus.ofAvailableQuantity(response.quantity() - response.reservedQuantity());
        } catch (RestClientException e) {
            // covers connection failures, timeouts, 4xx (e.g. no inventory record) and 5xx responses
            log.warn("Could not get stock status for product {}: {}", productId, e.getMessage());
            return StockStatus.UNKNOWN;
        }
    }

    /**
     * Provisional view of Inventory Service's InventoryResponse; other fields are ignored.
     */
    record InventoryResponse(Integer quantity, Integer reservedQuantity) {
    }
}
