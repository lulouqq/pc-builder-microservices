package com.csci318.orderservice.infrastructure;

import com.csci318.orderservice.service.OrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes the stock reservation result of an order from the Kafka topic
 * inventory-reservation-events (O3): StockReserved moves the order to PROCESSING,
 * StockReservationFailed to CANCELLED. The listener is not started when Kafka messaging is disabled.
 */
@Component
public class ReservationEventKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(ReservationEventKafkaListener.class);

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public ReservationEventKafkaListener(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.INVENTORY_RESERVATION_EVENTS, groupId = "order-service",
            autoStartup = "${messaging.kafka.enabled:true}")
    public void onMessage(String message) {
        JsonNode event;
        try {
            event = objectMapper.readTree(message);
        } catch (JsonProcessingException e) {
            // retrying cannot fix a message that is not JSON
            log.error("Skipping a reservation event that is not valid JSON: {}", message);
            return;
        }
        String eventType = event.path("eventType").asText();
        String orderId = event.path("orderId").asText();
        if (orderId.isBlank()) {
            log.error("Skipping {} without an orderId", eventType);
            return;
        }
        switch (eventType) {
            case "StockReserved" -> orderService.handleStockReserved(orderId);
            case "StockReservationFailed" -> orderService.handleStockReservationFailed(orderId);
            default -> log.warn("Ignoring unknown reservation event type '{}'", eventType);
        }
    }
}
