package com.csci318.catalogservice.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryUpdateRequest(@NotBlank String name) {
}
