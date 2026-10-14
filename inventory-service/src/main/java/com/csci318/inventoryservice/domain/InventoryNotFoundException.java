package com.csci318.inventoryservice.domain;

/**
 * Inventory item does not exist (InventoryNotFound; ResourceNotFound, 404, in the API error contract).
 */
public class InventoryNotFoundException extends RuntimeException {

    public InventoryNotFoundException(String productId) {
        super("No inventory record for product " + productId);
    }
}
