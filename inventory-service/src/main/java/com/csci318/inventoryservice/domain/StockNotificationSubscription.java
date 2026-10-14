package com.csci318.inventoryservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root (CRUD, not event-sourced): a customer's registration to receive stock
 * notifications for a product.
 */
@Entity
public class StockNotificationSubscription {

    @Id
    private String id;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private Instant createdAt;

    protected StockNotificationSubscription() {
    }

    public StockNotificationSubscription(String productId, String userId) {
        this.id = UUID.randomUUID().toString();
        this.productId = productId;
        this.userId = userId;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public String getUserId() {
        return userId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
