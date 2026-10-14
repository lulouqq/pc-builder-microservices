package com.csci318.orderservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Entity inside the Order aggregate: a product and its quantity. It is only changed through Order.
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    private String id;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Float unitPrice;

    protected OrderItem() {
    }

    OrderItem(String productId, Integer quantity, Float unitPrice) {
        this.id = UUID.randomUUID().toString();
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    void increaseQuantity(int additionalQuantity, Float currentUnitPrice) {
        this.quantity += additionalQuantity;
        this.unitPrice = currentUnitPrice;
    }

    public String getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Float getUnitPrice() {
        return unitPrice;
    }
}
