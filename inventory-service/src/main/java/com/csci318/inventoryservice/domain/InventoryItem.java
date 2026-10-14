package com.csci318.inventoryservice.domain;

import com.csci318.inventoryservice.domain.event.InventoryEvent;
import com.csci318.inventoryservice.domain.event.InventoryInitialized;
import com.csci318.inventoryservice.domain.event.StockLevelUpdated;
import com.csci318.inventoryservice.domain.event.StockReservationConfirmed;
import com.csci318.inventoryservice.domain.event.StockReservationReleased;
import com.csci318.inventoryservice.domain.event.StockReserved;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Event-sourced aggregate root: the stock record of a product. Its state is never stored directly;
 * it is rebuilt by replaying its events. Each behaviour checks the invariants and records an event,
 * and the state only changes when an event is applied.
 */
public class InventoryItem {

    public static final String AGGREGATE_TYPE = "InventoryItem";

    private final String id;
    private String productId;
    private int quantity;
    private int reservedQuantity;
    private long version;
    private final Map<String, Reservation> reservations = new HashMap<>();
    private final List<InventoryEvent> newEvents = new ArrayList<>();

    private InventoryItem(String id) {
        this.id = id;
    }

    /**
     * A product has one inventory item, so its id is derived from the product id.
     */
    public static String idFor(String productId) {
        return "inventory-item-" + productId;
    }

    /**
     * First creation of the inventory record of a product, with no stock.
     */
    public static InventoryItem initialize(String productId) {
        InventoryItem item = new InventoryItem(idFor(productId));
        item.record(new InventoryInitialized(productId, 0, 0));
        return item;
    }

    /**
     * Rebuilds an inventory item from its stored events, oldest first.
     */
    public static InventoryItem rehydrate(String id, List<InventoryEvent> history) {
        InventoryItem item = new InventoryItem(id);
        history.forEach(item::apply);
        return item;
    }

    public void updateStock(int newQuantity, String reason) {
        if (newQuantity < 0) {
            throw new ValidationFailedException("quantity must not be negative");
        }
        if (newQuantity < reservedQuantity) {
            throw new ValidationFailedException("quantity must not be lower than the reserved quantity ("
                    + reservedQuantity + ")");
        }
        record(new StockLevelUpdated(productId, quantity, newQuantity, reason));
    }

    /**
     * Reserves stock for an order and returns the id of the new reservation.
     */
    public String reserveStock(int requestedQuantity, String orderId) {
        if (requestedQuantity < 1) {
            throw new ValidationFailedException("quantity must be at least 1");
        }
        if (requestedQuantity > getAvailableQuantity()) {
            throw new StockUnavailableException("Only " + getAvailableQuantity() + " of product " + productId
                    + " available, " + requestedQuantity + " requested");
        }
        String reservationId = UUID.randomUUID().toString();
        record(new StockReserved(productId, orderId, reservationId, requestedQuantity));
        return reservationId;
    }

    public void releaseReservation(String reservationId) {
        Reservation reservation = activeReservation(reservationId);
        record(new StockReservationReleased(productId, reservation.orderId, reservationId, reservation.quantity));
    }

    public void confirmReservation(String reservationId) {
        Reservation reservation = activeReservation(reservationId);
        record(new StockReservationConfirmed(productId, reservation.orderId, reservationId, reservation.quantity));
    }

    private Reservation activeReservation(String reservationId) {
        Reservation reservation = reservations.get(reservationId);
        if (reservation == null) {
            throw new ReservationConflictException("Reservation " + reservationId + " does not exist for product "
                    + productId);
        }
        if (reservation.status != ReservationStatus.ACTIVE) {
            throw new ReservationConflictException("Reservation " + reservationId + " is already "
                    + reservation.status);
        }
        return reservation;
    }

    private void record(InventoryEvent event) {
        apply(event);
        newEvents.add(event);
    }

    private void apply(InventoryEvent event) {
        switch (event) {
            case InventoryInitialized e -> {
                productId = e.productId();
                quantity = e.quantity();
                reservedQuantity = e.reservedQuantity();
            }
            case StockLevelUpdated e -> quantity = e.newQuantity();
            case StockReserved e -> {
                reservedQuantity += e.quantity();
                reservations.put(e.reservationId(), new Reservation(e.orderId(), e.quantity()));
            }
            case StockReservationReleased e -> {
                reservedQuantity -= e.quantity();
                reservations.get(e.reservationId()).status = ReservationStatus.RELEASED;
            }
            case StockReservationConfirmed e -> {
                reservedQuantity -= e.quantity();
                quantity -= e.quantity();
                reservations.get(e.reservationId()).status = ReservationStatus.CONFIRMED;
            }
        }
        version++;
    }

    /**
     * Returns the events recorded since the item was loaded and clears them (called by the event store).
     */
    public List<InventoryEvent> pullNewEvents() {
        List<InventoryEvent> events = List.copyOf(newEvents);
        newEvents.clear();
        return events;
    }

    public String getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public int getAvailableQuantity() {
        return quantity - reservedQuantity;
    }

    /**
     * Number of events applied so far; the version of the last event in the stream.
     */
    public long getVersion() {
        return version;
    }

    private static final class Reservation {
        private final String orderId;
        private final int quantity;
        private ReservationStatus status = ReservationStatus.ACTIVE;

        private Reservation(String orderId, int quantity) {
            this.orderId = orderId;
            this.quantity = quantity;
        }
    }
}
