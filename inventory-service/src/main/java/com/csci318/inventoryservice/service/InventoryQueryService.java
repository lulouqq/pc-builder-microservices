package com.csci318.inventoryservice.service;

import com.csci318.inventoryservice.domain.InventoryNotFoundException;
import com.csci318.inventoryservice.domain.StockReservation;
import com.csci318.inventoryservice.infrastructure.InventoryReadModel;
import com.csci318.inventoryservice.infrastructure.InventoryReadModelRepository;
import com.csci318.inventoryservice.infrastructure.StockReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Query application service (specs/cqrs.md, 3.2): reads the projections only and never changes
 * InventoryItem state.
 */
@Service
@Transactional(readOnly = true)
public class InventoryQueryService {

    private final InventoryReadModelRepository readModels;
    private final StockReservationRepository reservations;

    public InventoryQueryService(InventoryReadModelRepository readModels, StockReservationRepository reservations) {
        this.readModels = readModels;
        this.reservations = reservations;
    }

    public InventoryReadModel handle(GetInventoryByProductQuery query) {
        return readModels.findById(query.productId())
                .orElseThrow(() -> new InventoryNotFoundException(query.productId()));
    }

    public StockReservation getReservation(String reservationId) {
        return reservations.findById(reservationId).orElseThrow();
    }
}
