package com.csci318.inventoryservice.domain.event;

import java.time.Instant;
import java.util.List;

/**
 * Stock could not be reserved for an order, so nothing was reserved for it. Published to Kafka as
 * StockReservationFailed for Order Service; it is not stored in the event store.
 */
public record OrderStockReservationFailed(String orderId, String reason, List<UnavailableItem> unavailableItems,
                                          Instant occurredAt) {

    public static final String REASON_STOCK_UNAVAILABLE = "STOCK_UNAVAILABLE";

    public record UnavailableItem(String productId, int requestedQuantity, int availableQuantity) {
    }
}
