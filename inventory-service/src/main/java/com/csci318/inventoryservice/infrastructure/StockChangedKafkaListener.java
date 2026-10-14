package com.csci318.inventoryservice.infrastructure;

import com.csci318.inventoryservice.domain.StockAlertType;
import com.csci318.inventoryservice.service.StockAlertService;
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
 * Consumes Inventory Service's own StockChanged from the Kafka topic inventory-stock-events and stores
 * stock alerts for the product's subscribers (I2, Architecture Decision 17). Only STOCK_LOW and
 * PRODUCT_RESTOCKED notify; STOCK_LEVEL_UPDATED is ignored. A message that cannot be read is skipped.
 * The listener is not started when Kafka messaging is disabled.
 */
@Component
public class StockChangedKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(StockChangedKafkaListener.class);

    private final StockAlertService stockAlertService;
    private final ObjectMapper objectMapper;

    public StockChangedKafkaListener(StockAlertService stockAlertService, ObjectMapper objectMapper) {
        this.stockAlertService = stockAlertService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.INVENTORY_STOCK_EVENTS, groupId = "inventory-stock-alerts",
            autoStartup = "${messaging.kafka.enabled:true}")
    public void onMessage(String message) {
        JsonNode event;
        try {
            event = objectMapper.readTree(message);
        } catch (JsonProcessingException e) {
            // retrying cannot fix a message that is not JSON
            log.error("Skipping a stock event that is not valid JSON: {}", message);
            return;
        }
        String eventType = event.path("eventType").asText();
        if (!"StockChanged".equals(eventType)) {
            log.warn("Ignoring unknown stock event type '{}'", eventType);
            return;
        }
        String change = event.path("change").asText();
        StockAlertType type;
        switch (change) {
            case "STOCK_LOW" -> type = StockAlertType.STOCK_LOW;
            case "PRODUCT_RESTOCKED" -> type = StockAlertType.PRODUCT_RESTOCKED;
            case "STOCK_LEVEL_UPDATED" -> {
                return;
            }
            default -> {
                log.warn("Ignoring StockChanged with unknown change '{}'", change);
                return;
            }
        }
        String eventId = event.path("eventId").asText();
        String productId = event.path("productId").asText();
        JsonNode quantity = event.path("quantity");
        JsonNode reservedQuantity = event.path("reservedQuantity");
        if (eventId.isBlank() || productId.isBlank() || !quantity.isInt() || !reservedQuantity.isInt()) {
            log.error("Skipping StockChanged without an eventId, productId, quantity or reservedQuantity: {}",
                    message);
            return;
        }
        Instant occurredAt;
        try {
            occurredAt = Instant.parse(event.path("occurredAt").asText());
        } catch (DateTimeParseException e) {
            log.error("Skipping StockChanged with an invalid occurredAt: {}", message);
            return;
        }
        stockAlertService.notifySubscribers(eventId, productId, type,
                quantity.asInt() - reservedQuantity.asInt(), occurredAt);
    }
}
