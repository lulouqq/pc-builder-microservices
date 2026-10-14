package com.csci318.orderservice.infrastructure;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topics of the order flow (Architecture Decision 14). All events of one order use the order
 * id as the message key, so they stay in order.
 */
@Configuration
public class KafkaTopics {

    /** OrderPlaced, OrderCancelled, OrderCompleted: published by Order Service. */
    public static final String ORDER_EVENTS = "order-events";

    /** StockReserved, StockReservationFailed: published by Inventory Service. */
    public static final String INVENTORY_RESERVATION_EVENTS = "inventory-reservation-events";

    /** Creates the topic this service publishes to, if the broker does not have it yet. */
    @Bean
    @ConditionalOnProperty(name = "messaging.kafka.enabled", havingValue = "true", matchIfMissing = true)
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(ORDER_EVENTS).partitions(1).replicas(1).build();
    }
}
