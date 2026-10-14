package com.csci318.catalogservice.infrastructure;

import com.csci318.catalogservice.domain.event.ProductViewed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes ProductViewed to the Kafka topic catalog-events as JSON, keyed by the product id, after the
 * product details request (C2) has completed. A failed send is logged and never fails the request.
 */
@Component
@ConditionalOnProperty(name = "messaging.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class ProductViewedKafkaPublisher {

    static final int EVENT_VERSION = 1;

    private static final Logger log = LoggerFactory.getLogger(ProductViewedKafkaPublisher.class);

    private final KafkaOperations<String, String> kafka;
    private final ObjectMapper objectMapper;

    public ProductViewedKafkaPublisher(KafkaOperations<String, String> kafka, ObjectMapper objectMapper) {
        this.kafka = kafka;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(ProductViewed event) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("eventId", UUID.randomUUID().toString());
        message.put("eventType", "ProductViewed");
        message.put("version", EVENT_VERSION);
        message.put("occurredAt", event.occurredAt().toString());
        message.put("productId", event.productId());
        try {
            kafka.send(KafkaTopics.CATALOG_EVENTS, event.productId(), objectMapper.writeValueAsString(message))
                    .whenComplete((result, failure) -> {
                        if (failure != null) {
                            log.error("Could not publish ProductViewed for product {}", event.productId(), failure);
                        }
                    });
        } catch (JsonProcessingException | RuntimeException e) {
            log.error("Could not publish ProductViewed for product {}", event.productId(), e);
        }
    }
}
