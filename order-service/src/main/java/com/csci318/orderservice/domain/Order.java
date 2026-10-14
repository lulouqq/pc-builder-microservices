package com.csci318.orderservice.domain;

import com.csci318.orderservice.domain.event.OrderCancelled;
import com.csci318.orderservice.domain.event.OrderCompleted;
import com.csci318.orderservice.domain.event.OrderPlaced;
import com.csci318.orderservice.domain.event.PaymentStatusUpdated;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root: a customer purchase. A DRAFT order is the customer's cart; items and the delivery
 * address can only be changed while it is DRAFT. Every status change goes through a method of this
 * class, which rejects it with OrderStateViolationException when it is not valid for the current
 * status. Domain events are registered here and published when the order is saved.
 */
@Entity
@Table(name = "orders")
public class Order extends AbstractAggregateRoot<Order> {

    @Id
    private String id;

    @Column(nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private Float totalPrice;

    @Column(nullable = false)
    private Instant createdAt;

    private String deliveryAddressId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    private Instant paymentDueAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    private Order(String userId) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.status = OrderStatus.DRAFT;
        this.totalPrice = 0f;
        this.createdAt = Instant.now();
        this.paymentStatus = PaymentStatus.PENDING;
    }

    /**
     * Creates an empty DRAFT order: the customer's cart.
     */
    public static Order createDraft(String userId) {
        return new Order(userId);
    }

    /**
     * Adds a product to the cart. If the product is already in the cart its quantity is increased,
     * and the unit price is updated to the given (current Catalog) price.
     */
    public void addItem(String productId, int quantity, Float unitPrice) {
        requireStatus("add items", OrderStatus.DRAFT);
        if (quantity < 1) {
            throw new ValidationFailedException("quantity must be at least 1");
        }
        items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .ifPresentOrElse(
                        item -> item.increaseQuantity(quantity, unitPrice),
                        () -> items.add(new OrderItem(productId, quantity, unitPrice)));
        recalculateTotalPrice();
    }

    /**
     * Removes an item from the cart; returns false when the order has no such item.
     */
    public boolean removeItem(String orderItemId) {
        requireStatus("remove items", OrderStatus.DRAFT);
        boolean removed = items.removeIf(item -> item.getId().equals(orderItemId));
        recalculateTotalPrice();
        return removed;
    }

    public void selectDeliveryAddress(String addressId) {
        requireStatus("select a delivery address", OrderStatus.DRAFT);
        this.deliveryAddressId = addressId;
    }

    /**
     * Checkout: DRAFT -> PLACED. The cart must not be empty and needs a delivery address.
     */
    public void checkout() {
        requireStatus("check out", OrderStatus.DRAFT);
        if (items.isEmpty()) {
            throw new OrderStateViolationException("An empty cart cannot be checked out");
        }
        if (deliveryAddressId == null) {
            throw new OrderStateViolationException("A delivery address must be selected before checkout");
        }
        status = OrderStatus.PLACED;
        registerEvent(OrderPlaced.of(this));
    }

    /**
     * Inventory reserved the stock: PLACED -> PROCESSING. Payment must be recorded before paymentDueAt.
     */
    public void stockReserved(Instant paymentDueAt) {
        requireStatus("mark stock as reserved", OrderStatus.PLACED);
        this.status = OrderStatus.PROCESSING;
        this.paymentDueAt = paymentDueAt;
    }

    /**
     * Inventory could not reserve the stock: PLACED -> CANCELLED. Nothing was reserved, so no
     * OrderCancelled is published.
     */
    public void stockReservationFailed() {
        requireStatus("mark the stock reservation as failed", OrderStatus.PLACED);
        this.status = OrderStatus.CANCELLED;
    }

    /**
     * Records the payment result of a PROCESSING order: PAID -> COMPLETED, FAILED -> CANCELLED.
     */
    public void recordPayment(PaymentStatus result) {
        if (result != PaymentStatus.PAID && result != PaymentStatus.FAILED) {
            throw new ValidationFailedException("paymentStatus must be PAID or FAILED");
        }
        requireStatus("record a payment", OrderStatus.PROCESSING);
        this.paymentStatus = result;
        registerEvent(PaymentStatusUpdated.of(this));
        if (result == PaymentStatus.PAID) {
            this.status = OrderStatus.COMPLETED;
            registerEvent(OrderCompleted.of(this));
        } else {
            this.status = OrderStatus.CANCELLED;
            registerEvent(OrderCancelled.of(this, OrderCancelled.REASON_PAYMENT_FAILED));
        }
    }

    /**
     * Customer cancels an order that is not yet fulfilled (O6).
     */
    public void cancel() {
        requireStatus("cancel", OrderStatus.DRAFT, OrderStatus.PLACED, OrderStatus.PROCESSING);
        cancelAndNotify(OrderCancelled.REASON_CUSTOMER_CANCELLED);
    }

    /**
     * No payment was recorded within the time limit: PROCESSING -> CANCELLED.
     */
    public void expirePayment() {
        requireStatus("expire the payment", OrderStatus.PROCESSING);
        cancelAndNotify(OrderCancelled.REASON_PAYMENT_TIMEOUT);
    }

    /** OrderCancelled is only published once checkout has started the inventory flow. */
    private void cancelAndNotify(String reason) {
        boolean inventoryFlowStarted = status != OrderStatus.DRAFT;
        this.status = OrderStatus.CANCELLED;
        if (inventoryFlowStarted) {
            registerEvent(OrderCancelled.of(this, reason));
        }
    }

    /**
     * Rejects an operation that is not valid for the current status.
     */
    public void requireStatus(String operation, OrderStatus... allowed) {
        for (OrderStatus allowedStatus : allowed) {
            if (status == allowedStatus) {
                return;
            }
        }
        throw new OrderStateViolationException("Cannot " + operation + " when the order is " + status);
    }

    private void recalculateTotalPrice() {
        double total = items.stream().mapToDouble(item -> item.getUnitPrice() * item.getQuantity()).sum();
        this.totalPrice = (float) total;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Float getTotalPrice() {
        return totalPrice;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getDeliveryAddressId() {
        return deliveryAddressId;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public Instant getPaymentDueAt() {
        return paymentDueAt;
    }

    public List<OrderItem> getItems() {
        return List.copyOf(items);
    }
}
