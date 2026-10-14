package com.csci318.orderservice.infrastructure;

import com.csci318.orderservice.domain.event.OrderCancelled;
import com.csci318.orderservice.domain.event.OrderCompleted;
import com.csci318.orderservice.domain.event.OrderPlaced;
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
 * Publishes the order events to the Kafka topic order-events as JSON, keyed by the order id. An
 * event is sent only after the transaction that changed the order has committed.
 */
@Component
@ConditionalOnProperty(name = "messaging.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class OrderEventKafkaPublisher {

    static final int EVENT_VERSION = 1;

    private static final Logger log = LoggerFactory.getLogger(OrderEventKafkaPublisher.class);

    private final KafkaOperations<String, String> kafka;
    private final ObjectMapper objectMapper;

    public OrderEventKafkaPublisher(KafkaOperations<String, String> kafka, ObjectMapper objectMapper) {
        this.kafka = kafka;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(OrderPlaced event) {
        Map<String, Object> message = envelope("OrderPlaced", event.occurredAt());
        message.put("orderId", event.orderId());
        message.put("userId", event.userId());
        message.put("items", event.items());
        send(event.orderId(), message);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(OrderCancelled event) {
        Map<String, Object> message = envelope("OrderCancelled", event.occurredAt());
        message.put("orderId", event.orderId());
        message.put("userId", event.userId());
        message.put("reason", event.reason());
        send(event.orderId(), message);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(OrderCompleted event) {
        Map<String, Object> message = envelope("OrderCompleted", event.occurredAt());
        message.put("orderId", event.orderId());
        message.put("userId", event.userId());
        send(event.orderId(), message);
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
     * The order is already saved when this runs, so a failed send is logged and not rethrown.
     */
    private void send(String orderId, Map<String, Object> message) {
        try {
            kafka.send(KafkaTopics.ORDER_EVENTS, orderId, objectMapper.writeValueAsString(message))
                    .whenComplete((result, failure) -> {
                        if (failure != null) {
                            log.error("Could not publish {} for order {}", message.get("eventType"), orderId, failure);
                        }
                    });
        } catch (JsonProcessingException | RuntimeException e) {
            log.error("Could not publish {} for order {}", message.get("eventType"), orderId, e);
        }
    }
}
