package com.csci318.inventoryservice;

import com.csci318.inventoryservice.domain.StockAlertType;
import com.csci318.inventoryservice.infrastructure.StockAlertRepository;
import com.csci318.inventoryservice.infrastructure.StockChangedKafkaListener;
import com.csci318.inventoryservice.infrastructure.StockNotificationSubscriptionRepository;
import com.csci318.inventoryservice.service.StockAlertService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * I2: how the StockChanged listener reads messages from inventory-stock-events (Architecture Decision 17),
 * without a broker.
 */
class StockChangedKafkaListenerTests {

    private static final Instant OCCURRED_AT = Instant.parse("2026-10-14T03:15:00Z");

    private final List<Notified> notified = new ArrayList<>();
    /** Records the calls instead of storing alerts (a concrete class, so it is not mocked). */
    private final StockAlertService stockAlertService = new StockAlertService(
            mock(StockNotificationSubscriptionRepository.class), mock(StockAlertRepository.class)) {
        @Override
        public int notifySubscribers(String sourceEventId, String productId, StockAlertType type,
                                     int availableQuantity, Instant occurredAt) {
            notified.add(new Notified(sourceEventId, productId, type, availableQuantity, occurredAt));
            return 0;
        }
    };
    private final StockChangedKafkaListener listener =
            new StockChangedKafkaListener(stockAlertService, new ObjectMapper());

    @Test
    void stockLowNotifiesWithTheAvailableQuantity() {
        listener.onMessage(stockChanged("event-1", "p1", 12, 9, "STOCK_LOW"));

        assertThat(notified).containsExactly(new Notified("event-1", "p1", StockAlertType.STOCK_LOW, 3, OCCURRED_AT));
    }

    @Test
    void productRestockedNotifies() {
        listener.onMessage(stockChanged("event-2", "p1", 20, 0, "PRODUCT_RESTOCKED"));

        assertThat(notified).containsExactly(
                new Notified("event-2", "p1", StockAlertType.PRODUCT_RESTOCKED, 20, OCCURRED_AT));
    }

    @Test
    void stockLevelUpdatedDoesNotNotify() {
        listener.onMessage(stockChanged("event-3", "p1", 20, 0, "STOCK_LEVEL_UPDATED"));

        assertThat(notified).isEmpty();
    }

    @Test
    void skipsMessagesItCannotUse() {
        for (String message : List.of(
                "not json",
                stockChanged("e", "p1", 5, 0, "STOCK_LOW").replace("StockChanged", "SomethingElse"),
                stockChanged("e", "p1", 5, 0, "SOMETHING_ELSE"),
                stockChanged("", "p1", 5, 0, "STOCK_LOW"),
                stockChanged("e", "", 5, 0, "STOCK_LOW"),
                stockChanged("e", "p1", 5, 0, "STOCK_LOW").replace("\"quantity\": 5", "\"quantity\": \"five\""),
                stockChanged("e", "p1", 5, 0, "STOCK_LOW").replace(", \"reservedQuantity\": 0", ""),
                stockChanged("e", "p1", 5, 0, "STOCK_LOW").replace(OCCURRED_AT.toString(), "yesterday"))) {
            listener.onMessage(message);
        }

        assertThat(notified).isEmpty();
    }

    /** A StockChanged message as InventoryKafkaPublisher sends it. */
    private static String stockChanged(String eventId, String productId, int quantity, int reservedQuantity,
                                       String change) {
        return "{\"eventId\": \"" + eventId + "\", \"eventType\": \"StockChanged\", \"version\": 1, "
                + "\"occurredAt\": \"" + OCCURRED_AT + "\", \"productId\": \"" + productId + "\", "
                + "\"quantity\": " + quantity + ", \"reservedQuantity\": " + reservedQuantity + ", "
                + "\"change\": \"" + change + "\"}";
    }

    private record Notified(String sourceEventId, String productId, StockAlertType type, int availableQuantity,
                            Instant occurredAt) {
    }
}
