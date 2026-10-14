package com.csci318.inventoryservice.service;

public record ReleaseStockReservationCommand(String productId, String reservationId) {
}
