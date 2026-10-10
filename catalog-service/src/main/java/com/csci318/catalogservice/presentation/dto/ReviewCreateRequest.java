package com.csci318.catalogservice.presentation.dto;

import com.csci318.catalogservice.domain.Review;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewCreateRequest(
        @NotBlank String userId,
        @NotNull @Min(Review.MIN_RATING) @Max(Review.MAX_RATING) Integer rating,
        @Size(max = Review.MAX_COMMENT_LENGTH) String comment) {
}
