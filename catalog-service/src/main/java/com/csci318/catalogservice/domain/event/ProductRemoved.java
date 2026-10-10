package com.csci318.catalogservice.domain.event;

import java.time.LocalDateTime;

/**
 * Domain event (internal to Catalog Service): a product was removed.
 */
public record ProductRemoved(String productId, LocalDateTime occurredAt) {

    public static ProductRemoved of(String productId) {
        return new ProductRemoved(productId, LocalDateTime.now());
    }
}
