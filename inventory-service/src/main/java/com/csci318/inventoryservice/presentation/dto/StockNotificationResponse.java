package com.csci318.inventoryservice.presentation.dto;

import com.csci318.inventoryservice.domain.StockNotificationSubscription;

import java.time.Instant;

public record StockNotificationResponse(String id, String productId, String userId, Instant createdAt) {

    public static StockNotificationResponse from(StockNotificationSubscription subscription) {
        return new StockNotificationResponse(subscription.getId(), subscription.getProductId(),
                subscription.getUserId(), subscription.getCreatedAt());
    }
}
