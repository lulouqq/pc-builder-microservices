package com.csci318.inventoryservice.domain.event;

/**
 * The stock level of a product was set to a new quantity (I3). The reason is optional.
 */
public record StockLevelUpdated(String productId, int previousQuantity, int newQuantity, String reason)
        implements InventoryEvent {
}
