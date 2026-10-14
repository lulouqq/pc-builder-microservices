package com.csci318.inventoryservice.service;

public record ConfirmStockReservationCommand(String productId, String reservationId) {
}
