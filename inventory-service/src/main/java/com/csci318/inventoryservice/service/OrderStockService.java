package com.csci318.inventoryservice.service;

import com.csci318.inventoryservice.domain.InventoryItem;
import com.csci318.inventoryservice.domain.ReservationStatus;
import com.csci318.inventoryservice.domain.StockReservation;
import com.csci318.inventoryservice.domain.event.OrderStockReservationFailed;
import com.csci318.inventoryservice.domain.event.OrderStockReservationFailed.UnavailableItem;
import com.csci318.inventoryservice.domain.event.OrderStockReserved;
import com.csci318.inventoryservice.infrastructure.InventoryEventStore;
import com.csci318.inventoryservice.infrastructure.StockReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The inventory side of the order flow (Architecture Decisions 4, 6 and 14): turns the order events
 * consumed from Kafka into the I1 commands. An order is one unit: its stock is reserved for every
 * item or for none.
 */
@Service
@Transactional
public class OrderStockService {

    private static final Logger log = LoggerFactory.getLogger(OrderStockService.class);

    private final InventoryCommandService commandService;
    private final InventoryEventStore eventStore;
    private final StockReservationRepository reservations;
    private final ApplicationEventPublisher eventPublisher;

    public OrderStockService(InventoryCommandService commandService, InventoryEventStore eventStore,
                             StockReservationRepository reservations, ApplicationEventPublisher eventPublisher) {
        this.commandService = commandService;
        this.eventStore = eventStore;
        this.reservations = reservations;
        this.eventPublisher = eventPublisher;
    }

    /**
     * OrderPlaced: issues a ReserveStockCommand for every item when all of them can be reserved and
     * raises OrderStockReserved. Otherwise nothing is reserved (no event is stored) and
     * OrderStockReservationFailed is raised. An order that already has reservations is skipped,
     * so a repeated OrderPlaced does not reserve twice.
     */
    public void reserveStock(String orderId, List<OrderLine> lines) {
        if (reservations.existsByOrderId(orderId)) {
            log.warn("Ignoring OrderPlaced for order {}: stock was already reserved for it", orderId);
            return;
        }
        Map<String, Integer> quantities = new LinkedHashMap<>();
        lines.forEach(line -> quantities.merge(line.productId(), line.quantity(), Integer::sum));

        List<UnavailableItem> unavailable = new ArrayList<>();
        quantities.forEach((productId, quantity) -> {
            int available = eventStore.load(InventoryItem.idFor(productId))
                    .map(InventoryItem::getAvailableQuantity)
                    .orElse(0);
            if (quantity < 1 || quantity > available) {
                unavailable.add(new UnavailableItem(productId, quantity, available));
            }
        });
        if (quantities.isEmpty() || !unavailable.isEmpty()) {
            eventPublisher.publishEvent(new OrderStockReservationFailed(orderId,
                    OrderStockReservationFailed.REASON_STOCK_UNAVAILABLE, unavailable, Instant.now()));
            return;
        }

        List<OrderStockReserved.Reservation> reserved = new ArrayList<>();
        quantities.forEach((productId, quantity) -> {
            String reservationId = commandService.handle(new ReserveStockCommand(productId, orderId, quantity));
            reserved.add(new OrderStockReserved.Reservation(reservationId, productId, quantity));
        });
        eventPublisher.publishEvent(new OrderStockReserved(orderId, reserved, Instant.now()));
    }

    /**
     * OrderCancelled: releases every active reservation of the order (none is a no-op).
     */
    public void releaseReservations(String orderId) {
        for (StockReservation reservation : reservations.findByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE)) {
            commandService.handle(
                    new ReleaseStockReservationCommand(reservation.getProductId(), reservation.getId()));
        }
    }

    /**
     * OrderCompleted: confirms every active reservation of the order (none is a no-op).
     */
    public void confirmReservations(String orderId) {
        for (StockReservation reservation : reservations.findByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE)) {
            commandService.handle(
                    new ConfirmStockReservationCommand(reservation.getProductId(), reservation.getId()));
        }
    }
}
