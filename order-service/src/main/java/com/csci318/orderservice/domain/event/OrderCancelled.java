package com.csci318.orderservice.domain.event;

import com.csci318.orderservice.domain.Order;

import java.time.Instant;

/**
 * A PLACED or PROCESSING order was cancelled: Inventory Service is to release its stock reservation.
 * Published inside the service for now; its Kafka contract is a draft waiting for approval.
 */
public record OrderCancelled(String orderId, String userId, String reason, Instant occurredAt) {

    public static final String REASON_CUSTOMER_CANCELLED = "CUSTOMER_CANCELLED";
    public static final String REASON_PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String REASON_PAYMENT_TIMEOUT = "PAYMENT_TIMEOUT";

    public static OrderCancelled of(Order order, String reason) {
        return new OrderCancelled(order.getId(), order.getUserId(), reason, Instant.now());
    }
}
