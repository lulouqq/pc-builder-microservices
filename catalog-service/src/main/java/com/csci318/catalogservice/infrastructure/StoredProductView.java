package com.csci318.catalogservice.infrastructure;

import com.csci318.catalogservice.domain.event.ProductViewed;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One recorded product view (C3, Architecture Decision 16): the input to the trending list. The id is
 * the event id of the ProductViewed message, so a message that is delivered twice is stored once.
 */
@Entity
@Table(name = "product_view", indexes = @Index(columnList = "occurred_at"))
public class StoredProductView {

    @Id
    private String eventId;

    @Column(nullable = false)
    private String productId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected StoredProductView() {
    }

    public StoredProductView(String eventId, String productId, Instant occurredAt) {
        this.eventId = eventId;
        this.productId = productId;
        this.occurredAt = occurredAt;
    }

    public ProductViewed toEvent() {
        return new ProductViewed(productId, occurredAt);
    }

    public String getEventId() {
        return eventId;
    }

    public String getProductId() {
        return productId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
