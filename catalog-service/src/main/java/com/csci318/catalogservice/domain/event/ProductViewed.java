package com.csci318.catalogservice.domain.event;

import java.time.LocalDateTime;

/**
 * Domain event: a product's details were viewed (input to the trending list, C3).
 *
 * PROVISIONAL: nothing publishes this event yet. Its Kafka schema, topic and producer are proposed
 * in the catalog-service README and are waiting for team approval.
 */
public record ProductViewed(String productId, LocalDateTime occurredAt) {
}
