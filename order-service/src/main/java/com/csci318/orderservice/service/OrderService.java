package com.csci318.orderservice.service;

import com.csci318.orderservice.domain.Order;
import com.csci318.orderservice.domain.OrderStateViolationException;
import com.csci318.orderservice.domain.OrderStatus;
import com.csci318.orderservice.domain.PaymentStatus;
import com.csci318.orderservice.infrastructure.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Application service for orders. Every customer-facing method takes the id of the authenticated
 * user (callerId) and checks that this user owns the order. Orders are saved through the repository
 * so that the domain events registered by the Order aggregate are published.
 */
@Service
@Transactional
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final CatalogClient catalogClient;
    private final AccountClient accountClient;
    private final RecommendationClient recommendationClient;
    private final Duration paymentTimeLimit;

    public OrderService(OrderRepository orderRepository, CatalogClient catalogClient, AccountClient accountClient,
                        RecommendationClient recommendationClient,
                        @Value("${order.payment-time-limit}") Duration paymentTimeLimit) {
        this.orderRepository = orderRepository;
        this.catalogClient = catalogClient;
        this.accountClient = accountClient;
        this.recommendationClient = recommendationClient;
        this.paymentTimeLimit = paymentTimeLimit;
    }

    /**
     * O1: creates the customer's cart. A customer can have only one DRAFT order.
     */
    public Order createDraft(String userId, String callerId) {
        requireOwner(userId, callerId);
        orderRepository.findFirstByUserIdAndStatus(userId, OrderStatus.DRAFT).ifPresent(draft -> {
            throw new OrderStateViolationException("The customer already has a cart: order " + draft.getId());
        });
        return orderRepository.save(Order.createDraft(userId));
    }

    /**
     * O1: adds a product to the cart at Catalog's current price.
     */
    public Order addItem(String orderId, String productId, int quantity, String callerId) {
        Order order = ownOrder(orderId, callerId);
        order.requireStatus("add items", OrderStatus.DRAFT);
        order.addItem(productId, quantity, currentPrice(productId));
        return orderRepository.save(order);
    }

    public void removeItem(String orderId, String orderItemId, String callerId) {
        Order order = ownOrder(orderId, callerId);
        if (!order.removeItem(orderItemId)) {
            throw new ResourceNotFoundException("Order item not found: " + orderItemId);
        }
        orderRepository.save(order);
    }

    /**
     * O2: the address must be one of the customer's saved addresses in Account Service.
     */
    public Order selectDeliveryAddress(String orderId, String addressId, String callerId, String authorization) {
        Order order = ownOrder(orderId, callerId);
        order.requireStatus("select a delivery address", OrderStatus.DRAFT);
        if (!accountClient.getSavedAddressIds(order.getUserId(), authorization).contains(addressId)) {
            throw new ResourceNotFoundException("Saved address not found: " + addressId);
        }
        order.selectDeliveryAddress(addressId);
        return orderRepository.save(order);
    }

    /**
     * O1: DRAFT -> PLACED; OrderPlaced starts the inventory flow, whose result arrives asynchronously.
     */
    public Order checkout(String orderId, String callerId) {
        Order order = ownOrder(orderId, callerId);
        order.checkout();
        return orderRepository.save(order);
    }

    /**
     * O4: records PAID (-> COMPLETED) or FAILED (-> CANCELLED) for a PROCESSING order.
     */
    public Order recordPayment(String orderId, PaymentStatus result, String callerId) {
        Order order = ownOrder(orderId, callerId);
        order.recordPayment(result);
        return orderRepository.save(order);
    }

    /**
     * O6.
     */
    public Order cancel(String orderId, String callerId) {
        Order order = ownOrder(orderId, callerId);
        order.cancel();
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public Order getOrder(String orderId, String callerId) {
        return ownOrder(orderId, callerId);
    }

    /**
     * O5: order history, newest first.
     */
    @Transactional(readOnly = true)
    public List<Order> getOrders(String userId, String callerId) {
        requireOwner(userId, callerId);
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * R3: adds every component of a recommended build to the customer's cart (quantity 1 each, at
     * Catalog's current price). The cart is created if the customer has none.
     */
    public Order createFromRecommendation(String recommendationId, String callerId, String authorization) {
        RecommendedBuild build = recommendationClient.getRecommendedBuild(recommendationId, authorization);
        requireOwner(build.userId(), callerId);
        Order order = orderRepository.findFirstByUserIdAndStatus(callerId, OrderStatus.DRAFT)
                .orElseGet(() -> Order.createDraft(callerId));
        for (String productId : build.productIds()) {
            order.addItem(productId, 1, currentPrice(productId));
        }
        return orderRepository.save(order);
    }

    /**
     * O3: Inventory Service reserved the stock (to be called when StockReserved is consumed).
     * Ignored when the order is no longer PLACED, e.g. it was cancelled in the meantime.
     */
    public void handleStockReserved(String orderId) {
        orderRepository.findById(orderId)
                .filter(order -> isPlaced(order, "StockReserved"))
                .ifPresent(order -> {
                    order.stockReserved(Instant.now().plus(paymentTimeLimit));
                    orderRepository.save(order);
                });
    }

    /**
     * O3: Inventory Service could not reserve the stock (to be called when StockReservationFailed
     * is consumed).
     */
    public void handleStockReservationFailed(String orderId) {
        orderRepository.findById(orderId)
                .filter(order -> isPlaced(order, "StockReservationFailed"))
                .ifPresent(order -> {
                    order.stockReservationFailed();
                    orderRepository.save(order);
                });
    }

    /**
     * FR-06: cancels PROCESSING orders whose payment time limit passed before the given time.
     * Returns the number of orders cancelled.
     */
    public int cancelOverduePayments(Instant now) {
        List<Order> overdue = orderRepository.findByStatusAndPaymentDueAtBefore(OrderStatus.PROCESSING, now);
        for (Order order : overdue) {
            order.expirePayment();
            orderRepository.save(order);
            log.info("Order {} cancelled: no payment recorded within the time limit", order.getId());
        }
        return overdue.size();
    }

    private boolean isPlaced(Order order, String event) {
        if (order.getStatus() != OrderStatus.PLACED) {
            log.warn("Ignoring {} for order {} in status {}", event, order.getId(), order.getStatus());
            return false;
        }
        return true;
    }

    private Float currentPrice(String productId) {
        return catalogClient.findProduct(productId)
                .map(CatalogProduct::price)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
    }

    /** Unknown order: 404. Another user's order: 403. */
    private Order ownOrder(String orderId, String callerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        requireOwner(order.getUserId(), callerId);
        return order;
    }

    private void requireOwner(String userId, String callerId) {
        if (userId == null || !userId.equals(callerId)) {
            throw new AccessDeniedException("You do not have permission to access this user's orders");
        }
    }
}
