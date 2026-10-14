package com.csci318.inventoryservice.infrastructure;

import com.csci318.inventoryservice.domain.StockNotificationSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockNotificationSubscriptionRepository extends JpaRepository<StockNotificationSubscription, String> {

    Optional<StockNotificationSubscription> findByProductIdAndUserId(String productId, String userId);
}
