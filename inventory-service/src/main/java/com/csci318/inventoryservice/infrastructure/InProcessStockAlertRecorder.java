package com.csci318.inventoryservice.infrastructure;

import com.csci318.inventoryservice.domain.StockAlertType;
import com.csci318.inventoryservice.domain.event.ProductRestocked;
import com.csci318.inventoryservice.domain.event.StockLow;
import com.csci318.inventoryservice.service.StockAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.UUID;

/**
 * When Kafka messaging is disabled, creates the stock alerts directly from StockLow and ProductRestocked,
 * without the inventory-stock-events topic (Architecture Decision 17). It runs after the stock change has
 * committed; a failure is logged and never fails the stock change.
 */
@Component
@ConditionalOnProperty(name = "messaging.kafka.enabled", havingValue = "false")
public class InProcessStockAlertRecorder {

    private static final Logger log = LoggerFactory.getLogger(InProcessStockAlertRecorder.class);

    private final StockAlertService stockAlertService;

    public InProcessStockAlertRecorder(StockAlertService stockAlertService) {
        this.stockAlertService = stockAlertService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(StockLow event) {
        createAlerts(event.productId(), StockAlertType.STOCK_LOW, event.availableQuantity(), event.occurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(ProductRestocked event) {
        createAlerts(event.productId(), StockAlertType.PRODUCT_RESTOCKED, event.availableQuantity(),
                event.occurredAt());
    }

    private void createAlerts(String productId, StockAlertType type, int availableQuantity, Instant occurredAt) {
        try {
            stockAlertService.notifySubscribers(UUID.randomUUID().toString(), productId, type, availableQuantity,
                    occurredAt);
        } catch (RuntimeException e) {
            log.error("Could not create {} alerts for product {}", type, productId, e);
        }
    }
}
