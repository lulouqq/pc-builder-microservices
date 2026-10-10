package com.csci318.catalogservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Aggregate root: a customer review and rating of a product.
 * The product and the user are referenced by id.
 */
@Entity
public class Review {

    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 5;
    public static final int MAX_COMMENT_LENGTH = 2000;

    @Id
    private String id;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private Integer rating;

    @Column(name = "review_comment", length = MAX_COMMENT_LENGTH)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Review() {
    }

    public Review(String productId, String userId, Integer rating, String comment) {
        if (rating == null || rating < MIN_RATING || rating > MAX_RATING) {
            throw new IllegalArgumentException("rating must be between " + MIN_RATING + " and " + MAX_RATING);
        }
        this.id = UUID.randomUUID().toString();
        this.productId = productId;
        this.userId = userId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public String getUserId() {
        return userId;
    }

    public Integer getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
