package com.csci318.inventoryservice.domain;

import com.csci318.inventoryservice.domain.event.ProductRestocked;
import com.csci318.inventoryservice.domain.event.StockLow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain service: derives StockLow and ProductRestocked from a change in available stock
 * (quantity - reservedQuantity). The low-stock threshold comes from configuration.
 */
public class StockAlertPolicy {

    private final int lowStockThreshold;

    public StockAlertPolicy(int lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    /**
     * StockLow: available stock was at or above the threshold and is now below it.
     * ProductRestocked: no stock was available and some is available now.
     */
    public List<Object> derivedEvents(String productId, int availableBefore, int availableAfter) {
        List<Object> events = new ArrayList<>();
        Instant now = Instant.now();
        if (availableBefore <= 0 && availableAfter > 0) {
            events.add(new ProductRestocked(productId, availableAfter, now));
        }
        if (availableBefore >= lowStockThreshold && availableAfter < lowStockThreshold) {
            events.add(new StockLow(productId, availableAfter, lowStockThreshold, now));
        }
        return events;
    }
}
