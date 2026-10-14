package com.csci318.inventoryservice.domain;

/**
 * Reservation is invalid or cannot be released or confirmed (ReservationConflict, 409).
 */
public class ReservationConflictException extends RuntimeException {

    public ReservationConflictException(String message) {
        super(message);
    }
}
