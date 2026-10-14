package com.csci318.inventoryservice.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * One row of the event store, with the event fields of specs/event-sourcing.md (3.1). Rows are only
 * ever inserted. The unique constraint on the stream version rejects two writers appending the same
 * version to one stream.
 */
@Entity
@Table(name = "inventory_event",
        uniqueConstraints = @UniqueConstraint(columnNames = {"aggregate_id", "aggregate_version"}))
public class StoredEvent {

    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "aggregate_version", nullable = false)
    private long aggregateVersion;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    protected StoredEvent() {
    }

    public StoredEvent(String eventId, String aggregateId, String aggregateType, String eventType,
                       long aggregateVersion, Instant occurredAt, String payload) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.aggregateVersion = aggregateVersion;
        this.occurredAt = occurredAt;
        this.payload = payload;
    }

    public String getEventId() {
        return eventId;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getEventType() {
        return eventType;
    }

    public long getAggregateVersion() {
        return aggregateVersion;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getPayload() {
        return payload;
    }
}
