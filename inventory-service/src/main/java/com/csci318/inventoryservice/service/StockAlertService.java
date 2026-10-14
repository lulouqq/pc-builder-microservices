package com.csci318.inventoryservice.service;

import com.csci318.inventoryservice.domain.StockAlert;
import com.csci318.inventoryservice.domain.StockAlertType;
import com.csci318.inventoryservice.domain.StockNotificationSubscription;
import com.csci318.inventoryservice.infrastructure.StockAlertRepository;
import com.csci318.inventoryservice.infrastructure.StockNotificationSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * I2 (Architecture Decision 17): turns a low-stock or restock change into a StockAlert for every customer
 * subscribed to the product. Called by the StockChanged Kafka listener, or in-process when Kafka
 * messaging is disabled.
 */
@Service
public class StockAlertService {

    private final StockNotificationSubscriptionRepository subscriptions;
    private final StockAlertRepository alerts;

    public StockAlertService(StockNotificationSubscriptionRepository subscriptions, StockAlertRepository alerts) {
        this.subscriptions = subscriptions;
        this.alerts = alerts;
    }

    /**
     * Stores one alert per subscription of the product, except for a customer who already has an alert
     * for this source event (a repeated message). Subscriptions stay active. Returns the number of new
     * alerts. Runs in its own transaction, because the in-process caller runs after the stock change
     * has committed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int notifySubscribers(String sourceEventId, String productId, StockAlertType type,
                                 int availableQuantity, Instant occurredAt) {
        int created = 0;
        for (StockNotificationSubscription subscription : subscriptions.findByProductId(productId)) {
            if (!alerts.existsBySourceEventIdAndUserId(sourceEventId, subscription.getUserId())) {
                alerts.save(new StockAlert(sourceEventId, subscription.getUserId(), productId, type,
                        availableQuantity, occurredAt));
                created++;
            }
        }
        return created;
    }
}
