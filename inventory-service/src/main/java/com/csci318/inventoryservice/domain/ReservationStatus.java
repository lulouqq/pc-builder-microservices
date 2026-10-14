package com.csci318.inventoryservice.domain;

/**
 * ACTIVE after StockReserved, RELEASED after StockReservationReleased, CONFIRMED after StockReservationConfirmed.
 */
public enum ReservationStatus {
    ACTIVE,
    RELEASED,
    CONFIRMED
}
