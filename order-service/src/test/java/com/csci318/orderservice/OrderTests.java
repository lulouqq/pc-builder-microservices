package com.csci318.orderservice;

import com.csci318.orderservice.domain.Order;
import com.csci318.orderservice.domain.OrderItem;
import com.csci318.orderservice.domain.OrderStateViolationException;
import com.csci318.orderservice.domain.OrderStatus;
import com.csci318.orderservice.domain.PaymentStatus;
import com.csci318.orderservice.domain.ValidationFailedException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The Order aggregate and its lifecycle, without Spring or a database.
 */
class OrderTests {

    private static final Instant DUE = Instant.parse("2026-10-14T10:10:00Z");

    @Test
    void newOrderIsAnEmptyDraft() {
        Order order = Order.createDraft("user-1");

        assertThat(order.getId()).isNotBlank();
        assertThat(order.getUserId()).isEqualTo("user-1");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DRAFT);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(order.getTotalPrice()).isZero();
        assertThat(order.getItems()).isEmpty();
        assertThat(order.getCreatedAt()).isNotNull();
        assertThat(order.getDeliveryAddressId()).isNull();
        assertThat(order.getPaymentDueAt()).isNull();
    }

    @Test
    void addingItemsUpdatesTheTotalPrice() {
        Order order = Order.createDraft("user-1");

        order.addItem("cpu", 1, 299f);
        order.addItem("ram", 2, 80f);

        assertThat(order.getItems()).extracting(OrderItem::getProductId).containsExactly("cpu", "ram");
        assertThat(order.getTotalPrice()).isEqualTo(459f);
    }

    @Test
    void addingAProductAgainIncreasesItsQuantityAndUsesTheNewPrice() {
        Order order = Order.createDraft("user-1");
        order.addItem("ram", 2, 80f);

        order.addItem("ram", 1, 90f);

        assertThat(order.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getQuantity()).isEqualTo(3);
            assertThat(item.getUnitPrice()).isEqualTo(90f);
        });
        assertThat(order.getTotalPrice()).isEqualTo(270f);
    }

    @Test
    void rejectsQuantityBelowOne() {
        Order order = Order.createDraft("user-1");

        assertThatThrownBy(() -> order.addItem("cpu", 0, 299f)).isInstanceOf(ValidationFailedException.class);
        assertThat(order.getItems()).isEmpty();
    }

    @Test
    void removingAnItemUpdatesTheTotalPrice() {
        Order order = Order.createDraft("user-1");
        order.addItem("cpu", 1, 299f);
        order.addItem("ram", 2, 80f);
        String cpuItemId = order.getItems().get(0).getId();

        assertThat(order.removeItem(cpuItemId)).isTrue();
        assertThat(order.removeItem("unknown")).isFalse();

        assertThat(order.getItems()).extracting(OrderItem::getProductId).containsExactly("ram");
        assertThat(order.getTotalPrice()).isEqualTo(160f);
    }

    @Test
    void checkoutNeedsItemsAndADeliveryAddress() {
        Order order = Order.createDraft("user-1");

        assertThatThrownBy(order::checkout).isInstanceOf(OrderStateViolationException.class);
        order.addItem("cpu", 1, 299f);
        assertThatThrownBy(order::checkout).isInstanceOf(OrderStateViolationException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DRAFT);

        order.selectDeliveryAddress("address-1");
        order.checkout();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void cartCannotBeChangedAfterCheckout() {
        Order order = placedOrder();
        String itemId = order.getItems().get(0).getId();

        assertThatThrownBy(() -> order.addItem("gpu", 1, 799f)).isInstanceOf(OrderStateViolationException.class);
        assertThatThrownBy(() -> order.removeItem(itemId)).isInstanceOf(OrderStateViolationException.class);
        assertThatThrownBy(() -> order.selectDeliveryAddress("address-2"))
                .isInstanceOf(OrderStateViolationException.class);
        assertThatThrownBy(order::checkout).isInstanceOf(OrderStateViolationException.class);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getDeliveryAddressId()).isEqualTo("address-1");
    }

    @Test
    void stockReservedMovesPlacedOrderToProcessing() {
        Order order = placedOrder();

        order.stockReserved(DUE);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(order.getPaymentDueAt()).isEqualTo(DUE);
        assertThatThrownBy(() -> order.stockReserved(DUE)).isInstanceOf(OrderStateViolationException.class);
    }

    @Test
    void failedReservationCancelsPlacedOrder() {
        Order order = placedOrder();

        order.stockReservationFailed();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void successfulPaymentCompletesTheOrder() {
        Order order = processingOrder();

        order.recordPayment(PaymentStatus.PAID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void failedPaymentCancelsTheOrder() {
        Order order = processingOrder();

        order.recordPayment(PaymentStatus.FAILED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void paymentIsOnlyRecordedForProcessingOrders() {
        Order draft = Order.createDraft("user-1");
        Order placed = placedOrder();
        Order completed = processingOrder();
        completed.recordPayment(PaymentStatus.PAID);

        assertThatThrownBy(() -> draft.recordPayment(PaymentStatus.PAID))
                .isInstanceOf(OrderStateViolationException.class);
        assertThatThrownBy(() -> placed.recordPayment(PaymentStatus.PAID))
                .isInstanceOf(OrderStateViolationException.class);
        assertThatThrownBy(() -> completed.recordPayment(PaymentStatus.FAILED))
                .isInstanceOf(OrderStateViolationException.class);
        assertThatThrownBy(() -> processingOrder().recordPayment(PaymentStatus.PENDING))
                .isInstanceOf(ValidationFailedException.class);
        assertThat(completed.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void unfulfilledOrdersCanBeCancelled() {
        Order draft = Order.createDraft("user-1");
        Order placed = placedOrder();
        Order processing = processingOrder();

        draft.cancel();
        placed.cancel();
        processing.cancel();

        assertThat(draft.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(placed.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(processing.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void completedOrCancelledOrdersCannotBeCancelled() {
        Order completed = processingOrder();
        completed.recordPayment(PaymentStatus.PAID);
        Order cancelled = placedOrder();
        cancelled.cancel();

        assertThatThrownBy(completed::cancel).isInstanceOf(OrderStateViolationException.class);
        assertThatThrownBy(cancelled::cancel).isInstanceOf(OrderStateViolationException.class);
        assertThat(completed.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void paymentTimeoutCancelsOnlyProcessingOrders() {
        Order processing = processingOrder();

        processing.expirePayment();

        assertThat(processing.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(processing.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThatThrownBy(() -> placedOrder().expirePayment()).isInstanceOf(OrderStateViolationException.class);
    }

    private Order placedOrder() {
        Order order = Order.createDraft("user-1");
        order.addItem("cpu", 1, 299f);
        order.selectDeliveryAddress("address-1");
        order.checkout();
        return order;
    }

    private Order processingOrder() {
        Order order = placedOrder();
        order.stockReserved(DUE);
        return order;
    }
}
