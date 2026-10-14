package com.csci318.orderservice.domain.event;

import com.csci318.orderservice.domain.Order;

import java.time.Instant;

/**
 * Payment was recorded as PAID (PROCESSING -> COMPLETED): Inventory Service is to confirm the stock
 * reservation. Published inside the service for now; its Kafka contract is a draft waiting for approval.
 */
public record OrderCompleted(String orderId, String userId, Instant occurredAt) {

    public static OrderCompleted of(Order order) {
        return new OrderCompleted(order.getId(), order.getUserId(), Instant.now());
    }
}
