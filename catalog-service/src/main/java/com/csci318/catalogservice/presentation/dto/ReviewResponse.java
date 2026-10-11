package com.csci318.catalogservice.presentation.dto;

import com.csci318.catalogservice.domain.Review;

import java.time.LocalDateTime;

public record ReviewResponse(String id, String productId, String userId, Integer rating, String comment,
                             LocalDateTime createdAt) {

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(review.getId(), review.getProductId(), review.getUserId(), review.getRating(),
                review.getComment(), review.getCreatedAt());
    }
}
