package com.csci318.catalogservice.service;

import com.csci318.catalogservice.domain.Review;
import com.csci318.catalogservice.domain.event.ProductRemoved;
import com.csci318.catalogservice.domain.event.ProductReviewed;
import com.csci318.catalogservice.infrastructure.ProductRepository;
import com.csci318.catalogservice.infrastructure.ReviewRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository,
                         ApplicationEventPublisher eventPublisher) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<Review> listReviews(String productId) {
        requireProductExists(productId);
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    public Review submitReview(String productId, String userId, Integer rating, String comment) {
        requireProductExists(productId);
        Review review = reviewRepository.save(new Review(productId, userId, rating, comment));
        eventPublisher.publishEvent(ProductReviewed.of(review));
        return review;
    }

    /**
     * A removed product's reviews are removed with it.
     */
    @EventListener
    public void onProductRemoved(ProductRemoved event) {
        reviewRepository.deleteByProductId(event.productId());
    }

    private void requireProductExists(String productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found: " + productId);
        }
    }
}
