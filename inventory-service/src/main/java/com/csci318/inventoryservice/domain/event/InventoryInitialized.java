package com.csci318.inventoryservice.domain.event;

/**
 * The inventory record of a product was created.
 */
public record InventoryInitialized(String productId, int quantity, int reservedQuantity) implements InventoryEvent {
}
