package com.csci318.inventoryservice.domain.event;

import java.time.Instant;

/**
 * Derived event: stock became available again after being unavailable (I2). Not stored in the event store.
 */
public record ProductRestocked(String productId, int availableQuantity, Instant occurredAt) {
}
