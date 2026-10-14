package com.csci318.inventoryservice.infrastructure;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topics used by Inventory Service (Architecture Decision 14).
 */
@Configuration
public class KafkaTopics {

    /** OrderPlaced, OrderCancelled, OrderCompleted: published by Order Service, keyed by order id. */
    public static final String ORDER_EVENTS = "order-events";

    /** StockReserved, StockReservationFailed: published by Inventory Service, keyed by order id. */
    public static final String INVENTORY_RESERVATION_EVENTS = "inventory-reservation-events";

    /** StockChanged: published by Inventory Service, keyed by product id. */
    public static final String INVENTORY_STOCK_EVENTS = "inventory-stock-events";

    /** Creates the topics this service publishes to, if the broker does not have them yet. */
    @Bean
    @ConditionalOnProperty(name = "messaging.kafka.enabled", havingValue = "true", matchIfMissing = true)
    public NewTopic inventoryReservationEventsTopic() {
        return TopicBuilder.name(INVENTORY_RESERVATION_EVENTS).partitions(1).replicas(1).build();
    }

    @Bean
    @ConditionalOnProperty(name = "messaging.kafka.enabled", havingValue = "true", matchIfMissing = true)
    public NewTopic inventoryStockEventsTopic() {
        return TopicBuilder.name(INVENTORY_STOCK_EVENTS).partitions(1).replicas(1).build();
    }
}
