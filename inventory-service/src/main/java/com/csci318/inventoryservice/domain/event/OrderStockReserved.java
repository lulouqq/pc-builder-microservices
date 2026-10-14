package com.csci318.inventoryservice.domain.event;

import java.time.Instant;
import java.util.List;

/**
 * Stock was reserved for every item of an order. Published to Kafka as StockReserved for Order
 * Service; the per-product StockReserved events are the ones stored in the event store.
 */
public record OrderStockReserved(String orderId, List<Reservation> reservations, Instant occurredAt) {

    public record Reservation(String reservationId, String productId, int quantity) {
    }
}
