package com.csci318.inventoryservice.presentation.dto;

import com.csci318.inventoryservice.domain.StockReservation;

public record StockReservationResponse(String id, String productId, String orderId, Integer quantity,
                                       String status) {

    public static StockReservationResponse from(StockReservation reservation) {
        return new StockReservationResponse(reservation.getId(), reservation.getProductId(),
                reservation.getOrderId(), reservation.getQuantity(), reservation.getStatus().name());
    }
}
