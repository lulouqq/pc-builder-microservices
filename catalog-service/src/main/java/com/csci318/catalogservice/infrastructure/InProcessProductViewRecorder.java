package com.csci318.catalogservice.infrastructure;

import com.csci318.catalogservice.domain.event.ProductViewed;
import com.csci318.catalogservice.service.TrendingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * When Kafka messaging is disabled, records ProductViewed for the trending list directly, without the
 * catalog-events topic (Architecture Decision 16). A failure is logged and never fails the request.
 */
@Component
@ConditionalOnProperty(name = "messaging.kafka.enabled", havingValue = "false")
public class InProcessProductViewRecorder {

    private static final Logger log = LoggerFactory.getLogger(InProcessProductViewRecorder.class);

    private final TrendingService trendingService;

    public InProcessProductViewRecorder(TrendingService trendingService) {
        this.trendingService = trendingService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(ProductViewed event) {
        try {
            trendingService.recordView(UUID.randomUUID().toString(), event);
        } catch (RuntimeException e) {
            log.error("Could not record a view of product {}", event.productId(), e);
        }
    }
}
