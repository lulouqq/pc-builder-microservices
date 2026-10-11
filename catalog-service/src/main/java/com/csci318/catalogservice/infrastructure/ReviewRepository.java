package com.csci318.catalogservice.infrastructure;

import com.csci318.catalogservice.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, String> {

    List<Review> findByProductIdOrderByCreatedAtDesc(String productId);

    void deleteByProductId(String productId);
}
