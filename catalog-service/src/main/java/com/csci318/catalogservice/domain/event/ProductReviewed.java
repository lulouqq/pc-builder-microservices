package com.csci318.catalogservice.domain.event;

import com.csci318.catalogservice.domain.Review;

import java.time.LocalDateTime;

/**
 * Domain event (internal to Catalog Service): a review and rating was submitted for a product.
 */
public record ProductReviewed(String reviewId, String productId, String userId, Integer rating,
                              LocalDateTime occurredAt) {

    public static ProductReviewed of(Review review) {
        return new ProductReviewed(review.getId(), review.getProductId(), review.getUserId(), review.getRating(),
                LocalDateTime.now());
    }
}
