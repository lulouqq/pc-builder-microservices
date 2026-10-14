package com.csci318.inventoryservice.domain.event;

/**
 * Stock was reserved for an order (I1); increases the reserved quantity.
 */
public record StockReserved(String productId, String orderId, String reservationId, int quantity)
        implements InventoryEvent {
}
