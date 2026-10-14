package com.csci318.inventoryservice.domain.event;

import java.time.Instant;

/**
 * Derived event: available stock fell below the configured threshold (I2). Not stored in the event store.
 */
public record StockLow(String productId, int availableQuantity, int threshold, Instant occurredAt) {
}
