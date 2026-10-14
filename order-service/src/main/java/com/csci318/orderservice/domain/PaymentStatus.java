package com.csci318.orderservice.domain;

/**
 * PENDING until a payment result is recorded (O4); only PAID and FAILED can be recorded.
 */
public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED
}
