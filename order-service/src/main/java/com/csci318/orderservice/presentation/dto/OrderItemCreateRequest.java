package com.csci318.orderservice.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemCreateRequest(@NotBlank String productId, @NotNull @Positive Integer quantity) {
}
