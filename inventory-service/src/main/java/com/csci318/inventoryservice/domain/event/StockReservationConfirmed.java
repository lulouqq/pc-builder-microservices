package com.csci318.inventoryservice.domain.event;

/**
 * A stock reservation was confirmed (I1): the reserved stock is sold, so both the reserved
 * quantity and the quantity decrease.
 */
public record StockReservationConfirmed(String productId, String orderId, String reservationId, int quantity)
        implements InventoryEvent {
}
