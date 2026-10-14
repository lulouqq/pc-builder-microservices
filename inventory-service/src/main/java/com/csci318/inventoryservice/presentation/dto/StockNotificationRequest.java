package com.csci318.inventoryservice.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record StockNotificationRequest(@NotBlank String userId) {
}
