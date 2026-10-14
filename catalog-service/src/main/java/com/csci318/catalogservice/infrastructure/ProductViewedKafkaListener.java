package com.csci318.catalogservice.infrastructure;

import com.csci318.catalogservice.domain.event.ProductViewed;
import com.csci318.catalogservice.service.TrendingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Consumes ProductViewed from the Kafka topic catalog-events and records the view for the trending list
 * (C3). A message that cannot be read is skipped. The listener is not started when Kafka messaging is
 * disabled.
 */
@Component
public class ProductViewedKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(ProductViewedKafkaListener.class);

    private final TrendingService trendingService;
    private final ObjectMapper objectMapper;

    public ProductViewedKafkaListener(TrendingService trendingService, ObjectMapper objectMapper) {
        this.trendingService = trendingService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.CATALOG_EVENTS, groupId = "catalog-service",
            autoStartup = "${messaging.kafka.enabled:true}")
    public void onMessage(String message) {
        JsonNode event;
        try {
            event = objectMapper.readTree(message);
        } catch (JsonProcessingException e) {
            // retrying cannot fix a message that is not JSON
            log.error("Skipping a catalog event that is not valid JSON: {}", message);
            return;
        }
        String eventType = event.path("eventType").asText();
        if (!"ProductViewed".equals(eventType)) {
            log.warn("Ignoring unknown catalog event type '{}'", eventType);
            return;
        }
        String eventId = event.path("eventId").asText();
        String productId = event.path("productId").asText();
        Instant occurredAt;
        try {
            occurredAt = Instant.parse(event.path("occurredAt").asText());
        } catch (DateTimeParseException e) {
            log.error("Skipping ProductViewed with an invalid occurredAt: {}", message);
            return;
        }
        if (eventId.isBlank() || productId.isBlank()) {
            log.error("Skipping ProductViewed without an eventId or productId: {}", message);
            return;
        }
        trendingService.recordView(eventId, new ProductViewed(productId, occurredAt));
    }
}
