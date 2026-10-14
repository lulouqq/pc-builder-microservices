package com.csci318.inventoryservice.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoredEventRepository extends JpaRepository<StoredEvent, String> {

    List<StoredEvent> findByAggregateIdOrderByAggregateVersionAsc(String aggregateId);

    boolean existsByAggregateId(String aggregateId);
}
