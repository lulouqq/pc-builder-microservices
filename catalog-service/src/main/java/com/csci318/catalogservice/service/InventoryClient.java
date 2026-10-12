package com.csci318.catalogservice.service;

import com.csci318.catalogservice.domain.StockStatus;

/**
 * Inter-service query to Inventory Service for a product's current stock status (C2).
 */
public interface InventoryClient {

    /**
     * Never throws: returns UNKNOWN when Inventory Service is unavailable or has no usable
     * stock information for the product.
     */
    StockStatus getStockStatus(String productId);
}
