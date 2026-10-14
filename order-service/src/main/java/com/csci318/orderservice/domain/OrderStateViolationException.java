package com.csci318.orderservice.domain;

/**
 * Requested operation is not valid for the current order state (OrderStateViolation, 409).
 */
public class OrderStateViolationException extends RuntimeException {

    public OrderStateViolationException(String message) {
        super(message);
    }
}
