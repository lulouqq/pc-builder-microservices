package com.csci318.inventoryservice.service;

import com.csci318.inventoryservice.domain.InventoryNotFoundException;
import com.csci318.inventoryservice.domain.StockAlert;
import com.csci318.inventoryservice.domain.StockReservation;
import com.csci318.inventoryservice.infrastructure.InventoryReadModel;
import com.csci318.inventoryservice.infrastructure.InventoryReadModelRepository;
import com.csci318.inventoryservice.infrastructure.StockAlertRepository;
import com.csci318.inventoryservice.infrastructure.StockReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Query application service (specs/cqrs.md, 3.2): reads the projections only and never changes
 * InventoryItem state.
 */
@Service
@Transactional(readOnly = true)
public class InventoryQueryService {

    private final InventoryReadModelRepository readModels;
    private final StockReservationRepository reservations;
    private final StockAlertRepository alerts;

    public InventoryQueryService(InventoryReadModelRepository readModels, StockReservationRepository reservations,
                                 StockAlertRepository alerts) {
        this.readModels = readModels;
        this.reservations = reservations;
        this.alerts = alerts;
    }

    public InventoryReadModel handle(GetInventoryByProductQuery query) {
        return readModels.findById(query.productId())
                .orElseThrow(() -> new InventoryNotFoundException(query.productId()));
    }

    /**
     * I2: the user's stock alerts, newest first; empty when there are none (users are not known here).
     */
    public List<StockAlert> handle(GetStockAlertsQuery query) {
        return alerts.findByUserIdOrderByOccurredAtDesc(query.userId());
    }

    public StockReservation getReservation(String reservationId) {
        return reservations.findById(reservationId).orElseThrow();
    }
}
