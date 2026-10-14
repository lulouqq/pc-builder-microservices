package com.csci318.inventoryservice;

import com.csci318.inventoryservice.domain.StockAlertPolicy;
import com.csci318.inventoryservice.domain.event.ProductRestocked;
import com.csci318.inventoryservice.domain.event.StockLow;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Derived events StockLow and ProductRestocked, with a threshold of 10.
 */
class StockAlertPolicyTests {

    private final StockAlertPolicy policy = new StockAlertPolicy(10);

    @Test
    void stockLowWhenAvailableStockFallsBelowTheThreshold() {
        List<Object> events = policy.derivedEvents("p1", 10, 9);

        assertThat(events).singleElement().isInstanceOfSatisfying(StockLow.class, stockLow -> {
            assertThat(stockLow.productId()).isEqualTo("p1");
            assertThat(stockLow.availableQuantity()).isEqualTo(9);
            assertThat(stockLow.threshold()).isEqualTo(10);
        });
        assertThat(policy.derivedEvents("p1", 50, 0)).hasOnlyElementsOfType(StockLow.class).hasSize(1);
    }

    @Test
    void noStockLowWhenStockStaysAboveOrWasAlreadyBelowTheThreshold() {
        assertThat(policy.derivedEvents("p1", 50, 10)).isEmpty();
        assertThat(policy.derivedEvents("p1", 9, 5)).isEmpty();
        assertThat(policy.derivedEvents("p1", 5, 9)).isEmpty();
        assertThat(policy.derivedEvents("p1", 5, 50)).isEmpty();
    }

    @Test
    void productRestockedWhenStockBecomesAvailableAgain() {
        List<Object> events = policy.derivedEvents("p1", 0, 25);

        assertThat(events).singleElement().isInstanceOfSatisfying(ProductRestocked.class, restocked -> {
            assertThat(restocked.productId()).isEqualTo("p1");
            assertThat(restocked.availableQuantity()).isEqualTo(25);
        });
        // restocked to a level that is still below the threshold: restocked, but no new StockLow
        assertThat(policy.derivedEvents("p1", 0, 3)).hasOnlyElementsOfType(ProductRestocked.class).hasSize(1);
    }

    @Test
    void noProductRestockedWhenStockWasAlreadyAvailable() {
        assertThat(policy.derivedEvents("p1", 1, 25)).isEmpty();
        assertThat(policy.derivedEvents("p1", 0, 0)).isEmpty();
    }

    @Test
    void thresholdIsConfigurable() {
        StockAlertPolicy lowThreshold = new StockAlertPolicy(3);

        assertThat(lowThreshold.derivedEvents("p1", 10, 5)).isEmpty();
        assertThat(lowThreshold.derivedEvents("p1", 5, 2)).hasOnlyElementsOfType(StockLow.class).hasSize(1);
    }
}
