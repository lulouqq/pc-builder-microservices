package com.csci318.catalogservice.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ProductViewRepository extends JpaRepository<StoredProductView, String> {

    List<StoredProductView> findByOccurredAtAfter(Instant since);
}
