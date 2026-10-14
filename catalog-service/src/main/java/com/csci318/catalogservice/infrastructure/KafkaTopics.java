package com.csci318.catalogservice.infrastructure;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topic of Catalog Service (Architecture Decision 16). ProductViewed is keyed by the product id.
 */
@Configuration
public class KafkaTopics {

    /** ProductViewed: published and consumed by Catalog Service (trending list, C3). */
    public static final String CATALOG_EVENTS = "catalog-events";

    /** Creates the topic this service publishes to, if the broker does not have it yet. */
    @Bean
    @ConditionalOnProperty(name = "messaging.kafka.enabled", havingValue = "true", matchIfMissing = true)
    public NewTopic catalogEventsTopic() {
        return TopicBuilder.name(CATALOG_EVENTS).partitions(1).replicas(1).build();
    }
}
