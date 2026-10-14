package com.csci318.inventoryservice.infrastructure;

import com.csci318.inventoryservice.domain.InventoryItem;
import com.csci318.inventoryservice.domain.event.InventoryEvent;
import com.csci318.inventoryservice.domain.event.InventoryInitialized;
import com.csci318.inventoryservice.domain.event.StockLevelUpdated;
import com.csci318.inventoryservice.domain.event.StockReservationConfirmed;
import com.csci318.inventoryservice.domain.event.StockReservationReleased;
import com.csci318.inventoryservice.domain.event.StockReserved;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Event store for InventoryItem streams: loads an item by replaying its events and appends the
 * events an item has recorded. The event type is the event class name and the payload is its JSON.
 */
@Component
public class InventoryEventStore {

    private static final Map<String, Class<? extends InventoryEvent>> EVENT_TYPES = Map.of(
            InventoryInitialized.class.getSimpleName(), InventoryInitialized.class,
            StockLevelUpdated.class.getSimpleName(), StockLevelUpdated.class,
            StockReserved.class.getSimpleName(), StockReserved.class,
            StockReservationReleased.class.getSimpleName(), StockReservationReleased.class,
            StockReservationConfirmed.class.getSimpleName(), StockReservationConfirmed.class);

    private final StoredEventRepository repository;
    private final ObjectMapper objectMapper;

    public InventoryEventStore(StoredEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public boolean exists(String aggregateId) {
        return repository.existsByAggregateId(aggregateId);
    }

    public Optional<InventoryItem> load(String aggregateId) {
        List<StoredEvent> stored = repository.findByAggregateIdOrderByAggregateVersionAsc(aggregateId);
        if (stored.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(InventoryItem.rehydrate(aggregateId, stored.stream().map(this::toEvent).toList()));
    }

    /**
     * Appends the item's new events to its stream and returns them.
     */
    public List<InventoryEvent> append(InventoryItem item) {
        List<InventoryEvent> events = item.pullNewEvents();
        long version = item.getVersion() - events.size();
        for (InventoryEvent event : events) {
            version++;
            repository.save(new StoredEvent(UUID.randomUUID().toString(), item.getId(),
                    InventoryItem.AGGREGATE_TYPE, event.getClass().getSimpleName(), version, Instant.now(),
                    toJson(event)));
        }
        return events;
    }

    private InventoryEvent toEvent(StoredEvent stored) {
        try {
            return objectMapper.readValue(stored.getPayload(), EVENT_TYPES.get(stored.getEventType()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot read event " + stored.getEventId(), e);
        }
    }

    private String toJson(InventoryEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot write event " + event, e);
        }
    }
}
