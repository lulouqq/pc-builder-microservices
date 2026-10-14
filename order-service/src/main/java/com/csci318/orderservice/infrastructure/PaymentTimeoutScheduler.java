package com.csci318.orderservice.infrastructure;

import com.csci318.orderservice.service.OrderService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;

/**
 * Scheduled timeout check (FR-06): cancels PROCESSING orders whose payment time limit has passed.
 */
@Configuration
@EnableScheduling
public class PaymentTimeoutScheduler {

    private final OrderService orderService;

    public PaymentTimeoutScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(fixedDelayString = "${order.payment-timeout-check-interval}")
    public void cancelOverdueOrders() {
        orderService.cancelOverduePayments(Instant.now());
    }
}
