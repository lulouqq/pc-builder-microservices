package com.csci318.inventoryservice.infrastructure;

import com.csci318.inventoryservice.service.OrderLine;
import com.csci318.inventoryservice.service.OrderStockService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Consumes the order events from the Kafka topic order-events: OrderPlaced reserves stock,
 * OrderCancelled releases the order's reservations and OrderCompleted confirms them. A message that
 * fails (for example because another order changed the same product at the same moment) is retried
 * by the listener container. The listener is not started when Kafka messaging is disabled.
 */
@Component
public class OrderEventKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventKafkaListener.class);

    private final OrderStockService orderStockService;
    private final ObjectMapper objectMapper;

    public OrderEventKafkaListener(OrderStockService orderStockService, ObjectMapper objectMapper) {
        this.orderStockService = orderStockService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.ORDER_EVENTS, groupId = "inventory-service",
            autoStartup = "${messaging.kafka.enabled:true}")
    public void onMessage(String message) {
        JsonNode event;
        try {
            event = objectMapper.readTree(message);
        } catch (JsonProcessingException e) {
            // retrying cannot fix a message that is not JSON
            log.error("Skipping an order event that is not valid JSON: {}", message);
            return;
        }
        String eventType = event.path("eventType").asText();
        String orderId = event.path("orderId").asText();
        if (orderId.isBlank()) {
            log.error("Skipping {} without an orderId", eventType);
            return;
        }
        switch (eventType) {
            case "OrderPlaced" -> orderStockService.reserveStock(orderId, orderLines(event.path("items")));
            case "OrderCancelled" -> orderStockService.releaseReservations(orderId);
            case "OrderCompleted" -> orderStockService.confirmReservations(orderId);
            default -> log.warn("Ignoring unknown order event type '{}'", eventType);
        }
    }

    private List<OrderLine> orderLines(JsonNode items) {
        List<OrderLine> lines = new ArrayList<>();
        for (JsonNode item : items) {
            lines.add(new OrderLine(item.path("productId").asText(), item.path("quantity").asInt()));
        }
        return lines;
    }
}
