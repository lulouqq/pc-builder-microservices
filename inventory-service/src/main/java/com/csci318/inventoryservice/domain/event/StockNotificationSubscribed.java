package com.csci318.inventoryservice.domain.event;

import java.time.Instant;

/**
 * A customer registered to receive stock notifications for a product (I2). Not part of the
 * InventoryItem event stream.
 */
public record StockNotificationSubscribed(String subscriptionId, String productId, String userId,
                                          Instant occurredAt) {
}
