package com.csci318.inventoryservice.presentation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * quantity is the new absolute stock level; reason is optional and is recorded in StockLevelUpdated.
 */
public record StockUpdateRequest(@NotNull @PositiveOrZero Integer quantity, String reason) {
}
