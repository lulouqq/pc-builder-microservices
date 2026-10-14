package com.csci318.inventoryservice;

import com.csci318.inventoryservice.domain.InventoryItem;
import com.csci318.inventoryservice.domain.ReservationConflictException;
import com.csci318.inventoryservice.domain.StockUnavailableException;
import com.csci318.inventoryservice.domain.ValidationFailedException;
import com.csci318.inventoryservice.domain.event.InventoryEvent;
import com.csci318.inventoryservice.domain.event.InventoryInitialized;
import com.csci318.inventoryservice.domain.event.StockLevelUpdated;
import com.csci318.inventoryservice.domain.event.StockReservationConfirmed;
import com.csci318.inventoryservice.domain.event.StockReservationReleased;
import com.csci318.inventoryservice.domain.event.StockReserved;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The event-sourced InventoryItem aggregate, without Spring or a database.
 */
class InventoryItemTests {

    @Test
    void initializesWithNoStock() {
        InventoryItem item = InventoryItem.initialize("p1");

        assertThat(item.getId()).isEqualTo("inventory-item-p1");
        assertThat(item.getProductId()).isEqualTo("p1");
        assertThat(item.getQuantity()).isZero();
        assertThat(item.getReservedQuantity()).isZero();
        assertThat(item.getVersion()).isEqualTo(1);
        assertThat(item.pullNewEvents()).containsExactly(new InventoryInitialized("p1", 0, 0));
    }

    @Test
    void updateStockSetsTheAbsoluteQuantity() {
        InventoryItem item = itemWithStock(10);

        item.updateStock(4, "stocktake");

        assertThat(item.getQuantity()).isEqualTo(4);
        assertThat(item.pullNewEvents()).containsExactly(new StockLevelUpdated("p1", 10, 4, "stocktake"));
    }

    @Test
    void rejectsNegativeQuantityAndQuantityBelowReserved() {
        InventoryItem item = itemWithStock(10);
        item.reserveStock(6, "order-1");
        item.pullNewEvents();

        assertThatThrownBy(() -> item.updateStock(-1, null)).isInstanceOf(ValidationFailedException.class);
        assertThatThrownBy(() -> item.updateStock(5, null)).isInstanceOf(ValidationFailedException.class);
        assertThat(item.pullNewEvents()).isEmpty();
        assertThat(item.getQuantity()).isEqualTo(10);

        // equal to the reserved quantity is allowed
        item.updateStock(6, null);
        assertThat(item.getAvailableQuantity()).isZero();
    }

    @Test
    void reservingIncreasesReservedQuantity() {
        InventoryItem item = itemWithStock(10);

        String reservationId = item.reserveStock(3, "order-1");

        assertThat(item.getQuantity()).isEqualTo(10);
        assertThat(item.getReservedQuantity()).isEqualTo(3);
        assertThat(item.getAvailableQuantity()).isEqualTo(7);
        assertThat(item.pullNewEvents()).containsExactly(new StockReserved("p1", "order-1", reservationId, 3));
    }

    @Test
    void cannotReserveMoreThanIsAvailable() {
        InventoryItem item = itemWithStock(10);
        item.reserveStock(8, "order-1");
        item.pullNewEvents();

        assertThatThrownBy(() -> item.reserveStock(3, "order-2")).isInstanceOf(StockUnavailableException.class);
        assertThatThrownBy(() -> item.reserveStock(0, "order-2")).isInstanceOf(ValidationFailedException.class);
        assertThat(item.pullNewEvents()).isEmpty();
        assertThat(item.getReservedQuantity()).isEqualTo(8);

        // exactly the available quantity can be reserved
        item.reserveStock(2, "order-2");
        assertThat(item.getAvailableQuantity()).isZero();
    }

    @Test
    void releasingDecreasesReservedQuantityOnly() {
        InventoryItem item = itemWithStock(10);
        String reservationId = item.reserveStock(3, "order-1");
        item.pullNewEvents();

        item.releaseReservation(reservationId);

        assertThat(item.getQuantity()).isEqualTo(10);
        assertThat(item.getReservedQuantity()).isZero();
        assertThat(item.pullNewEvents())
                .containsExactly(new StockReservationReleased("p1", "order-1", reservationId, 3));
    }

    @Test
    void confirmingDecreasesReservedQuantityAndQuantity() {
        InventoryItem item = itemWithStock(10);
        String reservationId = item.reserveStock(3, "order-1");
        item.pullNewEvents();

        item.confirmReservation(reservationId);

        assertThat(item.getQuantity()).isEqualTo(7);
        assertThat(item.getReservedQuantity()).isZero();
        assertThat(item.pullNewEvents())
                .containsExactly(new StockReservationConfirmed("p1", "order-1", reservationId, 3));
    }

    @Test
    void reservationCanOnlyBeReleasedOrConfirmedOnce() {
        InventoryItem item = itemWithStock(10);
        String released = item.reserveStock(1, "order-1");
        String confirmed = item.reserveStock(1, "order-2");
        item.releaseReservation(released);
        item.confirmReservation(confirmed);
        item.pullNewEvents();

        assertThatThrownBy(() -> item.releaseReservation(released)).isInstanceOf(ReservationConflictException.class);
        assertThatThrownBy(() -> item.confirmReservation(released)).isInstanceOf(ReservationConflictException.class);
        assertThatThrownBy(() -> item.releaseReservation(confirmed)).isInstanceOf(ReservationConflictException.class);
        assertThatThrownBy(() -> item.releaseReservation("unknown")).isInstanceOf(ReservationConflictException.class);
        assertThat(item.pullNewEvents()).isEmpty();
    }

    @Test
    void stateIsRebuiltByReplayingEvents() {
        List<InventoryEvent> history = new ArrayList<>();
        InventoryItem item = InventoryItem.initialize("p1");
        item.updateStock(20, null);
        String released = item.reserveStock(5, "order-1");
        String confirmed = item.reserveStock(4, "order-2");
        String active = item.reserveStock(2, "order-3");
        item.releaseReservation(released);
        item.confirmReservation(confirmed);
        history.addAll(item.pullNewEvents());

        InventoryItem replayed = InventoryItem.rehydrate(item.getId(), history);

        assertThat(replayed.getProductId()).isEqualTo("p1");
        assertThat(replayed.getQuantity()).isEqualTo(16);
        assertThat(replayed.getReservedQuantity()).isEqualTo(2);
        assertThat(replayed.getVersion()).isEqualTo(7);
        assertThat(replayed.pullNewEvents()).isEmpty();
        // reservation states are rebuilt too
        assertThatThrownBy(() -> replayed.releaseReservation(released))
                .isInstanceOf(ReservationConflictException.class);
        replayed.releaseReservation(active);
        assertThat(replayed.getReservedQuantity()).isZero();
        assertThat(replayed.getVersion()).isEqualTo(8);
    }

    private InventoryItem itemWithStock(int quantity) {
        InventoryItem item = InventoryItem.initialize("p1");
        item.updateStock(quantity, null);
        item.pullNewEvents();
        return item;
    }
}
