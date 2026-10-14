package com.csci318.orderservice.infrastructure;

import com.csci318.orderservice.domain.Order;
import com.csci318.orderservice.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<Order> findFirstByUserIdAndStatus(String userId, OrderStatus status);

    List<Order> findByStatusAndPaymentDueAtBefore(OrderStatus status, Instant time);
}
