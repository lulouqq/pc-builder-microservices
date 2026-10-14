package com.csci318.recommendationservice.service;

/**
 * Port to Inventory Service (public stock endpoint).
 */
public interface InventoryClient {

    /**
     * Whether the product has available stock. A product without an inventory record is not in stock.
     *
     * @throws ServiceUnavailableException when Inventory Service is unavailable or fails
     */
    boolean isInStock(String productId);
}
