package com.csci318.orderservice.domain.event;

import com.csci318.orderservice.domain.Order;

import java.time.Instant;

/**
 * Domain event (internal to Order Service): a payment result was recorded for an order (O4).
 */
public record PaymentStatusUpdated(String orderId, String paymentStatus, Instant occurredAt) {

    public static PaymentStatusUpdated of(Order order) {
        return new PaymentStatusUpdated(order.getId(), order.getPaymentStatus().name(), Instant.now());
    }
}
