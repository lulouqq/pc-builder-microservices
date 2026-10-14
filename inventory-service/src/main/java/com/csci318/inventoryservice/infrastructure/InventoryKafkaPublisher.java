package com.csci318.inventoryservice.infrastructure;

import com.csci318.inventoryservice.domain.event.OrderStockReservationFailed;
import com.csci318.inventoryservice.domain.event.OrderStockReserved;
import com.csci318.inventoryservice.domain.event.ProductRestocked;
import com.csci318.inventoryservice.domain.event.StockLevelUpdated;
import com.csci318.inventoryservice.domain.event.StockLow;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes Inventory Service's Kafka events as JSON: the reservation result of an order
 * (StockReserved / StockReservationFailed, keyed by order id) and StockChanged (keyed by product
 * id). An event is sent only after the transaction that caused it has committed.
 */
@Component
@ConditionalOnProperty(name = "messaging.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class InventoryKafkaPublisher {

    static final int EVENT_VERSION = 1;

    private static final Logger log = LoggerFactory.getLogger(InventoryKafkaPublisher.class);

    private final KafkaOperations<String, String> kafka;
    private final ObjectMapper objectMapper;
    private final InventoryReadModelRepository readModels;

    public InventoryKafkaPublisher(KafkaOperations<String, String> kafka, ObjectMapper objectMapper,
                                   InventoryReadModelRepository readModels) {
        this.kafka = kafka;
        this.objectMapper = objectMapper;
        this.readModels = readModels;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(OrderStockReserved event) {
        Map<String, Object> message = envelope("StockReserved", event.occurredAt());
        message.put("orderId", event.orderId());
        message.put("reservations", event.reservations());
        send(KafkaTopics.INVENTORY_RESERVATION_EVENTS, event.orderId(), message);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(OrderStockReservationFailed event) {
        Map<String, Object> message = envelope("StockReservationFailed", event.occurredAt());
        message.put("orderId", event.orderId());
        message.put("reason", event.reason());
        message.put("unavailableItems", event.unavailableItems());
        send(KafkaTopics.INVENTORY_RESERVATION_EVENTS, event.orderId(), message);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(StockLevelUpdated event) {
        publishStockChanged(event.productId(), "STOCK_LEVEL_UPDATED", Instant.now());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(StockLow event) {
        publishStockChanged(event.productId(), "STOCK_LOW", event.occurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(ProductRestocked event) {
        publishStockChanged(event.productId(), "PRODUCT_RESTOCKED", event.occurredAt());
    }

    /** StockChanged carries the product's current stock from the read model. */
    private void publishStockChanged(String productId, String change, Instant occurredAt) {
        readModels.findById(productId).ifPresent(stock -> {
            Map<String, Object> message = envelope("StockChanged", occurredAt);
            message.put("productId", productId);
            message.put("quantity", stock.getQuantity());
            message.put("reservedQuantity", stock.getReservedQuantity());
            message.put("change", change);
            send(KafkaTopics.INVENTORY_STOCK_EVENTS, productId, message);
        });
    }

    /** The fields every event has: eventId, eventType, version, occurredAt. */
    private Map<String, Object> envelope(String eventType, Instant occurredAt) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("eventId", UUID.randomUUID().toString());
        message.put("eventType", eventType);
        message.put("version", EVENT_VERSION);
        message.put("occurredAt", occurredAt.toString());
        return message;
    }

    /**
     * The stock change is already saved when this runs, so a failed send is logged and not rethrown.
     */
    private void send(String topic, String key, Map<String, Object> message) {
        try {
            kafka.send(topic, key, objectMapper.writeValueAsString(message))
                    .whenComplete((result, failure) -> {
                        if (failure != null) {
                            log.error("Could not publish {} for {}", message.get("eventType"), key, failure);
                        }
                    });
        } catch (JsonProcessingException | RuntimeException e) {
            log.error("Could not publish {} for {}", message.get("eventType"), key, e);
        }
    }
}
