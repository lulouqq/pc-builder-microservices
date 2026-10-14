package com.csci318.inventoryservice.infrastructure;

import com.csci318.inventoryservice.domain.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockAlertRepository extends JpaRepository<StockAlert, String> {

    boolean existsBySourceEventIdAndUserId(String sourceEventId, String userId);

    List<StockAlert> findByUserIdOrderByOccurredAtDesc(String userId);
}
