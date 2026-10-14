package com.csci318.inventoryservice.presentation.dto;

import com.csci318.inventoryservice.domain.StockAlert;
import com.csci318.inventoryservice.domain.StockAlertType;

import java.time.Instant;

public record StockAlertResponse(String id, String userId, String productId, StockAlertType type,
                                 int availableQuantity, String message, Instant occurredAt) {

    public static StockAlertResponse from(StockAlert alert) {
        return new StockAlertResponse(alert.getId(), alert.getUserId(), alert.getProductId(), alert.getType(),
                alert.getAvailableQuantity(), alert.getMessage(), alert.getOccurredAt());
    }
}
