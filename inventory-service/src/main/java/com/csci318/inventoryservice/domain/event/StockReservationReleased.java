package com.csci318.inventoryservice.domain.event;

/**
 * A stock reservation was released (I1); decreases the reserved quantity.
 */
public record StockReservationReleased(String productId, String orderId, String reservationId, int quantity)
        implements InventoryEvent {
}
