package com.csci318.catalogservice.presentation;

import com.csci318.catalogservice.presentation.dto.ReviewCreateRequest;
import com.csci318.catalogservice.presentation.dto.ReviewResponse;
import com.csci318.catalogservice.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/products/{productId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public List<ReviewResponse> listReviews(@PathVariable String productId) {
        return reviewService.listReviews(productId).stream().map(ReviewResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse submitReview(@PathVariable String productId,
                                       @Valid @RequestBody ReviewCreateRequest request,
                                       @AuthenticationPrincipal Jwt token) {
        // owner check: customers can only review as themselves (checked before the product is looked up)
        if (!token.getSubject().equals(request.userId())) {
            throw new AccessDeniedException("Token does not belong to user " + request.userId());
        }
        return ReviewResponse.from(reviewService.submitReview(productId, request.userId(), request.rating(),
                request.comment()));
    }
}
