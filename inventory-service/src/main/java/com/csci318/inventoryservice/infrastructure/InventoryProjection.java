package com.csci318.inventoryservice.infrastructure;

import com.csci318.inventoryservice.domain.InventoryItem;
import com.csci318.inventoryservice.domain.StockReservation;
import com.csci318.inventoryservice.domain.event.InventoryInitialized;
import com.csci318.inventoryservice.domain.event.StockLevelUpdated;
import com.csci318.inventoryservice.domain.event.StockReservationConfirmed;
import com.csci318.inventoryservice.domain.event.StockReservationReleased;
import com.csci318.inventoryservice.domain.event.StockReserved;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Projection handler: updates inventory_read_model and the stock reservations from InventoryItem
 * events. It runs in the transaction of the command that recorded the events.
 */
@Component
public class InventoryProjection {

    private final InventoryReadModelRepository readModels;
    private final StockReservationRepository reservations;

    public InventoryProjection(InventoryReadModelRepository readModels, StockReservationRepository reservations) {
        this.readModels = readModels;
        this.reservations = reservations;
    }

    @EventListener
    public void on(InventoryInitialized event) {
        readModels.save(new InventoryReadModel(event.productId(), InventoryItem.idFor(event.productId()),
                event.quantity(), event.reservedQuantity()));
    }

    @EventListener
    public void on(StockLevelUpdated event) {
        InventoryReadModel readModel = readModel(event.productId());
        readModel.setQuantity(event.newQuantity());
        readModels.save(readModel);
    }

    @EventListener
    public void on(StockReserved event) {
        InventoryReadModel readModel = readModel(event.productId());
        readModel.setReservedQuantity(readModel.getReservedQuantity() + event.quantity());
        readModels.save(readModel);
        reservations.save(new StockReservation(event.reservationId(), event.productId(), event.orderId(),
                event.quantity()));
    }

    @EventListener
    public void on(StockReservationReleased event) {
        InventoryReadModel readModel = readModel(event.productId());
        readModel.setReservedQuantity(readModel.getReservedQuantity() - event.quantity());
        readModels.save(readModel);
        StockReservation reservation = reservations.findById(event.reservationId()).orElseThrow();
        reservation.markReleased();
        reservations.save(reservation);
    }

    @EventListener
    public void on(StockReservationConfirmed event) {
        InventoryReadModel readModel = readModel(event.productId());
        readModel.setReservedQuantity(readModel.getReservedQuantity() - event.quantity());
        readModel.setQuantity(readModel.getQuantity() - event.quantity());
        readModels.save(readModel);
        StockReservation reservation = reservations.findById(event.reservationId()).orElseThrow();
        reservation.markConfirmed();
        reservations.save(reservation);
    }

    private InventoryReadModel readModel(String productId) {
        return readModels.findById(productId).orElseThrow();
    }
}
