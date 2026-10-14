package com.csci318.inventoryservice.domain.event;

/**
 * An event in the InventoryItem event stream (specs/event-sourcing.md, 3.3). Each record holds the
 * event's payload; the envelope fields (eventId, aggregateVersion, occurredAt, ...) are added by
 * the event store.
 */
public sealed interface InventoryEvent
        permits InventoryInitialized, StockLevelUpdated, StockReserved, StockReservationReleased,
        StockReservationConfirmed {

    String productId();
}
