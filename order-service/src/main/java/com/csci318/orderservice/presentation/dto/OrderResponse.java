package com.csci318.orderservice.presentation.dto;

import com.csci318.orderservice.domain.Order;
import com.csci318.orderservice.domain.OrderItem;

import java.time.Instant;
import java.util.List;

/**
 * paymentDueAt is set while the order waits for payment (from PROCESSING on); null before that.
 */
public record OrderResponse(String id, String userId, String status, Float totalPrice, Instant createdAt,
                            String deliveryAddressId, String paymentStatus, Instant paymentDueAt,
                            List<OrderItemResponse> items) {

    public record OrderItemResponse(String id, String productId, Integer quantity, Float unitPrice) {

        static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(item.getId(), item.getProductId(), item.getQuantity(), item.getUnitPrice());
        }
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getUserId(), order.getStatus().name(), order.getTotalPrice(),
                order.getCreatedAt(), order.getDeliveryAddressId(), order.getPaymentStatus().name(),
                order.getPaymentDueAt(), order.getItems().stream().map(OrderItemResponse::from).toList());
    }
}
