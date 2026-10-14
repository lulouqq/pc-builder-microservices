package com.csci318.orderservice.presentation;

import com.csci318.orderservice.presentation.dto.DeliveryAddressRequest;
import com.csci318.orderservice.presentation.dto.OrderCreateRequest;
import com.csci318.orderservice.presentation.dto.OrderItemCreateRequest;
import com.csci318.orderservice.presentation.dto.OrderResponse;
import com.csci318.orderservice.presentation.dto.PaymentStatusRequest;
import com.csci318.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The token's subject is the authenticated user; OrderService checks that this user owns the order.
 */
@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@Valid @RequestBody OrderCreateRequest request,
                                     @AuthenticationPrincipal Jwt token) {
        return OrderResponse.from(orderService.createDraft(request.userId(), token.getSubject()));
    }

    @PostMapping("/orders/{orderId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse addItem(@PathVariable String orderId, @Valid @RequestBody OrderItemCreateRequest request,
                                 @AuthenticationPrincipal Jwt token) {
        return OrderResponse.from(
                orderService.addItem(orderId, request.productId(), request.quantity(), token.getSubject()));
    }

    @DeleteMapping("/orders/{orderId}/items/{orderItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(@PathVariable String orderId, @PathVariable String orderItemId,
                           @AuthenticationPrincipal Jwt token) {
        orderService.removeItem(orderId, orderItemId, token.getSubject());
    }

    /** 202: the order is PLACED; the stock reservation result arrives asynchronously. */
    @PostMapping("/orders/{orderId}/checkout")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OrderResponse checkout(@PathVariable String orderId, @AuthenticationPrincipal Jwt token) {
        return OrderResponse.from(orderService.checkout(orderId, token.getSubject()));
    }

    @PutMapping("/orders/{orderId}/delivery-address")
    public OrderResponse selectDeliveryAddress(@PathVariable String orderId,
                                               @Valid @RequestBody DeliveryAddressRequest request,
                                               @AuthenticationPrincipal Jwt token,
                                               @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return OrderResponse.from(orderService.selectDeliveryAddress(orderId, request.addressId(),
                token.getSubject(), authorization));
    }

    @PutMapping("/orders/{orderId}/payment")
    public OrderResponse recordPayment(@PathVariable String orderId, @Valid @RequestBody PaymentStatusRequest request,
                                       @AuthenticationPrincipal Jwt token) {
        return OrderResponse.from(orderService.recordPayment(orderId, request.paymentStatus(), token.getSubject()));
    }

    @GetMapping("/orders/{orderId}")
    public OrderResponse getOrder(@PathVariable String orderId, @AuthenticationPrincipal Jwt token) {
        return OrderResponse.from(orderService.getOrder(orderId, token.getSubject()));
    }

    @GetMapping("/users/{userId}/orders")
    public List<OrderResponse> getOrders(@PathVariable String userId, @AuthenticationPrincipal Jwt token) {
        return orderService.getOrders(userId, token.getSubject()).stream().map(OrderResponse::from).toList();
    }

    @PostMapping("/orders/{orderId}/cancel")
    public OrderResponse cancel(@PathVariable String orderId, @AuthenticationPrincipal Jwt token) {
        return OrderResponse.from(orderService.cancel(orderId, token.getSubject()));
    }

    @PostMapping("/orders/from-recommendation/{recommendationId}")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createFromRecommendation(@PathVariable String recommendationId,
                                                  @AuthenticationPrincipal Jwt token,
                                                  @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return OrderResponse.from(
                orderService.createFromRecommendation(recommendationId, token.getSubject(), authorization));
    }
}
