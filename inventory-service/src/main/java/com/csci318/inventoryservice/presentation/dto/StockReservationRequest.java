package com.csci318.inventoryservice.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockReservationRequest(@NotBlank String orderId, @NotNull @Positive Integer quantity) {
}
