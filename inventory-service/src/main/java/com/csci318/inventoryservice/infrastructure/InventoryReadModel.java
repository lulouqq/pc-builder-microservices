package com.csci318.inventoryservice.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Read model (specs/cqrs.md, 4.2): the current stock of a product, updated from InventoryItem
 * events by InventoryProjection and read by the queries.
 */
@Entity
@Table(name = "inventory_read_model")
public class InventoryReadModel {

    @Id
    private String productId;

    @Column(nullable = false)
    private String inventoryItemId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int reservedQuantity;

    protected InventoryReadModel() {
    }

    public InventoryReadModel(String productId, String inventoryItemId, int quantity, int reservedQuantity) {
        this.productId = productId;
        this.inventoryItemId = inventoryItemId;
        this.quantity = quantity;
        this.reservedQuantity = reservedQuantity;
    }

    void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    void setReservedQuantity(int reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public String getProductId() {
        return productId;
    }

    public String getInventoryItemId() {
        return inventoryItemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }
}
