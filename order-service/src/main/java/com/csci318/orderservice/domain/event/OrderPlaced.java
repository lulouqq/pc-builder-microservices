package com.csci318.orderservice.domain.event;

import com.csci318.orderservice.domain.Order;

import java.time.Instant;
import java.util.List;

/**
 * Checkout completed (DRAFT -> PLACED): Inventory Service is to reserve stock for every item.
 * Published inside the service for now; its Kafka contract is a draft waiting for approval.
 */
public record OrderPlaced(String orderId, String userId, List<Item> items, Instant occurredAt) {

    public record Item(String productId, int quantity) {
    }

    public static OrderPlaced of(Order order) {
        return new OrderPlaced(order.getId(), order.getUserId(),
                order.getItems().stream().map(item -> new Item(item.getProductId(), item.getQuantity())).toList(),
                Instant.now());
    }
}
