package com.csci318.catalogservice.domain.event;

import java.time.Instant;

/**
 * Domain event: a product's details were viewed (C2). It is the input to the trending list (C3) and the
 * only Catalog event published to Kafka (topic catalog-events, Architecture Decisions 9 and 16).
 */
public record ProductViewed(String productId, Instant occurredAt) {

    public static ProductViewed of(String productId) {
        return new ProductViewed(productId, Instant.now());
    }
}
