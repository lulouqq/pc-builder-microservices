package com.csci318.inventoryservice;

import com.csci318.inventoryservice.domain.StockAlert;
import com.csci318.inventoryservice.domain.StockAlertType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * I2: the StockAlert entity and its generated message.
 */
class StockAlertTests {

    private static final Instant OCCURRED_AT = Instant.parse("2026-10-14T03:15:00Z");

    @Test
    void stockLowAlertHasTheChangeAndALowStockMessage() {
        StockAlert alert = new StockAlert("event-1", "user-1", "p1", StockAlertType.STOCK_LOW, 3, OCCURRED_AT);

        assertThat(alert.getId()).isNotBlank();
        assertThat(alert.getSourceEventId()).isEqualTo("event-1");
        assertThat(alert.getUserId()).isEqualTo("user-1");
        assertThat(alert.getProductId()).isEqualTo("p1");
        assertThat(alert.getType()).isEqualTo(StockAlertType.STOCK_LOW);
        assertThat(alert.getAvailableQuantity()).isEqualTo(3);
        assertThat(alert.getOccurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(alert.getMessage()).isEqualTo("Product p1 is running low: 3 left in stock");
    }

    @Test
    void restockAlertHasARestockMessage() {
        StockAlert alert = new StockAlert("event-1", "user-1", "p1", StockAlertType.PRODUCT_RESTOCKED, 20,
                OCCURRED_AT);

        assertThat(alert.getMessage()).isEqualTo("Product p1 is back in stock: 20 available");
    }

    @Test
    void everyAlertGetsItsOwnId() {
        StockAlert first = new StockAlert("event-1", "user-1", "p1", StockAlertType.STOCK_LOW, 3, OCCURRED_AT);
        StockAlert second = new StockAlert("event-1", "user-2", "p1", StockAlertType.STOCK_LOW, 3, OCCURRED_AT);

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }
}
