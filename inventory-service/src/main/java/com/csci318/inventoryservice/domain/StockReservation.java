package com.csci318.inventoryservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

/**
 * Entity: stock allocated to an order. It is a projection of the InventoryItem reservation events
 * and is only changed by them.
 */
@Entity
public class StockReservation {

    @Id
    private String id;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    protected StockReservation() {
    }

    public StockReservation(String id, String productId, String orderId, Integer quantity) {
        this.id = id;
        this.productId = productId;
        this.orderId = orderId;
        this.quantity = quantity;
        this.status = ReservationStatus.ACTIVE;
    }

    public void markReleased() {
        this.status = ReservationStatus.RELEASED;
    }

    public void markConfirmed() {
        this.status = ReservationStatus.CONFIRMED;
    }

    public String getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public String getOrderId() {
        return orderId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public ReservationStatus getStatus() {
        return status;
    }
}
