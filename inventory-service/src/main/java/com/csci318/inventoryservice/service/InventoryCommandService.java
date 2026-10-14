package com.csci318.inventoryservice.service;

import com.csci318.inventoryservice.domain.InventoryItem;
import com.csci318.inventoryservice.domain.InventoryNotFoundException;
import com.csci318.inventoryservice.domain.StockAlertPolicy;
import com.csci318.inventoryservice.domain.StockNotificationSubscription;
import com.csci318.inventoryservice.domain.event.StockNotificationSubscribed;
import com.csci318.inventoryservice.infrastructure.InventoryEventStore;
import com.csci318.inventoryservice.infrastructure.StockNotificationSubscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command application service (specs/cqrs.md, 3.1): each handle method loads the InventoryItem from
 * the event store, runs its behaviour, appends the resulting events and publishes them for the
 * projection. It does not read the read model.
 */
@Service
@Transactional
public class InventoryCommandService {

    private final InventoryEventStore eventStore;
    private final StockNotificationSubscriptionRepository subscriptions;
    private final ApplicationEventPublisher eventPublisher;
    private final StockAlertPolicy stockAlertPolicy;

    public InventoryCommandService(InventoryEventStore eventStore,
                                   StockNotificationSubscriptionRepository subscriptions,
                                   ApplicationEventPublisher eventPublisher,
                                   @Value("${inventory.low-stock-threshold}") int lowStockThreshold) {
        this.eventStore = eventStore;
        this.subscriptions = subscriptions;
        this.eventPublisher = eventPublisher;
        this.stockAlertPolicy = new StockAlertPolicy(lowStockThreshold);
    }

    /**
     * I3. The first stock update for a product creates its inventory record (InventoryInitialized).
     */
    public void handle(UpdateStockLevelCommand command) {
        InventoryItem item = eventStore.load(InventoryItem.idFor(command.productId()))
                .orElseGet(() -> InventoryItem.initialize(command.productId()));
        int availableBefore = item.getAvailableQuantity();
        item.updateStock(command.quantity(), command.reason());
        commit(item, availableBefore);
    }

    /**
     * I1. Returns the id of the new reservation.
     */
    public String handle(ReserveStockCommand command) {
        InventoryItem item = load(command.productId());
        int availableBefore = item.getAvailableQuantity();
        String reservationId = item.reserveStock(command.quantity(), command.orderId());
        commit(item, availableBefore);
        return reservationId;
    }

    public void handle(ReleaseStockReservationCommand command) {
        InventoryItem item = load(command.productId());
        int availableBefore = item.getAvailableQuantity();
        item.releaseReservation(command.reservationId());
        commit(item, availableBefore);
    }

    public void handle(ConfirmStockReservationCommand command) {
        InventoryItem item = load(command.productId());
        int availableBefore = item.getAvailableQuantity();
        item.confirmReservation(command.reservationId());
        commit(item, availableBefore);
    }

    /**
     * I2 (CRUD, not through the aggregate). Subscribing twice returns the existing subscription.
     */
    public StockNotificationSubscription handle(SubscribeStockNotificationCommand command) {
        if (!eventStore.exists(InventoryItem.idFor(command.productId()))) {
            throw new InventoryNotFoundException(command.productId());
        }
        return subscriptions.findByProductIdAndUserId(command.productId(), command.userId())
                .orElseGet(() -> {
                    StockNotificationSubscription subscription = subscriptions.save(
                            new StockNotificationSubscription(command.productId(), command.userId()));
                    eventPublisher.publishEvent(new StockNotificationSubscribed(subscription.getId(),
                            subscription.getProductId(), subscription.getUserId(), subscription.getCreatedAt()));
                    return subscription;
                });
    }

    private InventoryItem load(String productId) {
        return eventStore.load(InventoryItem.idFor(productId))
                .orElseThrow(() -> new InventoryNotFoundException(productId));
    }

    /**
     * Appends the item's new events, publishes them for the projection, and publishes the derived
     * StockLow / ProductRestocked events for the change in available stock.
     */
    private void commit(InventoryItem item, int availableBefore) {
        eventStore.append(item).forEach(eventPublisher::publishEvent);
        stockAlertPolicy.derivedEvents(item.getProductId(), availableBefore, item.getAvailableQuantity())
                .forEach(eventPublisher::publishEvent);
    }
}
