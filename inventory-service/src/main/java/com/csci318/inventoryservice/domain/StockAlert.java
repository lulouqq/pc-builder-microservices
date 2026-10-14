package com.csci318.inventoryservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity (not event-sourced): a stock notification stored for a subscribed customer when the product's
 * stock becomes low or is restocked (I2, Architecture Decision 17). One StockChanged creates at most one
 * alert per customer.
 */
@Entity
@Table(name = "stock_alert",
        uniqueConstraints = @UniqueConstraint(columnNames = {"source_event_id", "user_id"}),
        indexes = @Index(columnList = "user_id"))
public class StockAlert {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockAlertType type;

    @Column(nullable = false)
    private Integer availableQuantity;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(name = "source_event_id", nullable = false)
    private String sourceEventId;

    protected StockAlert() {
    }

    public StockAlert(String sourceEventId, String userId, String productId, StockAlertType type,
                      int availableQuantity, Instant occurredAt) {
        this.id = UUID.randomUUID().toString();
        this.sourceEventId = sourceEventId;
        this.userId = userId;
        this.productId = productId;
        this.type = type;
        this.availableQuantity = availableQuantity;
        this.message = messageFor(type, productId, availableQuantity);
        this.occurredAt = occurredAt;
    }

    /** The alert text; the product is named by its id, because Inventory Service does not call Catalog. */
    static String messageFor(StockAlertType type, String productId, int availableQuantity) {
        return switch (type) {
            case STOCK_LOW -> "Product " + productId + " is running low: " + availableQuantity + " left in stock";
            case PRODUCT_RESTOCKED -> "Product " + productId + " is back in stock: " + availableQuantity + " available";
        };
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getProductId() {
        return productId;
    }

    public StockAlertType getType() {
        return type;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public String getMessage() {
        return message;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }
}
