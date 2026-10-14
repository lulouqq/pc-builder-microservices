package com.csci318.orderservice.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record OrderCreateRequest(@NotBlank String userId) {
}
