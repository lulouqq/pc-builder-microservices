package com.csci318.orderservice;

import com.csci318.orderservice.domain.Order;
import com.csci318.orderservice.domain.OrderStatus;
import com.csci318.orderservice.domain.event.OrderCancelled;
import com.csci318.orderservice.domain.event.OrderCompleted;
import com.csci318.orderservice.domain.event.OrderPlaced;
import com.csci318.orderservice.infrastructure.OrderEventKafkaPublisher;
import com.csci318.orderservice.infrastructure.ReservationEventKafkaListener;
import com.csci318.orderservice.service.AccountClient;
import com.csci318.orderservice.service.CatalogClient;
import com.csci318.orderservice.service.CatalogProduct;
import com.csci318.orderservice.service.OrderService;
import com.csci318.orderservice.service.RecommendationClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * The Kafka messages of the order flow (Architecture Decision 14) without a broker: the JSON that
 * Order Service publishes to order-events, and what it does with the JSON it consumes from
 * inventory-reservation-events.
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderKafkaMessagingTests {

    private static final Instant OCCURRED_AT = Instant.parse("2026-10-14T03:15:00Z");

    @Autowired
    private ReservationEventKafkaListener listener;

    @Autowired
    private OrderService orderService;

    @MockBean
    private CatalogClient catalogClient;

    @MockBean
    private AccountClient accountClient;

    @MockBean
    private RecommendationClient recommendationClient;

    private String userId;
    private String orderId;

    @BeforeEach
    void placeAnOrder() {
        userId = "user-" + UUID.randomUUID();
        given(catalogClient.findProduct("cpu")).willReturn(Optional.of(new CatalogProduct("cpu", "Ryzen 5", 299f)));
        given(accountClient.getSavedAddressIds(userId, "Bearer token")).willReturn(List.of("address-1"));
        orderId = orderService.createDraft(userId, userId).getId();
        orderService.addItem(orderId, "cpu", 2, userId);
        orderService.selectDeliveryAddress(orderId, "address-1", userId, "Bearer token");
        orderService.checkout(orderId, userId);
    }

    // --- consumed: inventory-reservation-events ---

    @Test
    void stockReservedMovesTheOrderToProcessing() {
        listener.onMessage(reservationEvent("StockReserved", orderId,
                "\"reservations\": [{\"reservationId\": \"r1\", \"productId\": \"cpu\", \"quantity\": 2}]"));

        Order order = orderService.getOrder(orderId, userId);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(order.getPaymentDueAt()).isAfter(Instant.now());
    }

    @Test
    void stockReservationFailedCancelsTheOrder() {
        listener.onMessage(reservationEvent("StockReservationFailed", orderId,
                "\"reason\": \"STOCK_UNAVAILABLE\", \"unavailableItems\": "
                        + "[{\"productId\": \"cpu\", \"requestedQuantity\": 2, \"availableQuantity\": 0}]"));

        assertThat(orderService.getOrder(orderId, userId).getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void repeatedOrUnusableMessagesAreSkipped() {
        listener.onMessage("not json");
        listener.onMessage("{\"eventType\": \"StockReserved\"}");
        listener.onMessage(reservationEvent("SomethingElse", orderId, "\"x\": 1"));
        listener.onMessage(reservationEvent("StockReserved", "unknown-order", "\"reservations\": []"));
        assertThat(orderService.getOrder(orderId, userId).getStatus()).isEqualTo(OrderStatus.PLACED);

        // a result that arrives twice, or after the first one, changes nothing
        listener.onMessage(reservationEvent("StockReserved", orderId, "\"reservations\": []"));
        listener.onMessage(reservationEvent("StockReserved", orderId, "\"reservations\": []"));
        listener.onMessage(reservationEvent("StockReservationFailed", orderId, "\"reason\": \"STOCK_UNAVAILABLE\""));
        assertThat(orderService.getOrder(orderId, userId).getStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    // --- published: order-events ---

    @Test
    @SuppressWarnings("unchecked")
    void publishesOrderEventsAsJsonKeyedByOrderId() {
        KafkaOperations<String, String> kafka = mock(KafkaOperations.class);
        given(kafka.send(anyString(), anyString(), anyString())).willReturn(new CompletableFuture<>());
        OrderEventKafkaPublisher publisher = new OrderEventKafkaPublisher(kafka, new ObjectMapper());

        publisher.on(new OrderPlaced("order-1", "user-1",
                List.of(new OrderPlaced.Item("cpu", 1), new OrderPlaced.Item("ram", 2)), OCCURRED_AT));
        publisher.on(new OrderCancelled("order-1", "user-1", "PAYMENT_TIMEOUT", OCCURRED_AT));
        publisher.on(new OrderCompleted("order-1", "user-1", OCCURRED_AT));

        ArgumentCaptor<String> topics = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messages = ArgumentCaptor.forClass(String.class);
        verify(kafka, org.mockito.Mockito.times(3)).send(topics.capture(), keys.capture(), messages.capture());
        assertThat(topics.getAllValues()).containsOnly("order-events");
        assertThat(keys.getAllValues()).containsOnly("order-1");

        String placed = messages.getAllValues().get(0);
        assertThat((String) JsonPath.read(placed, "$.eventType")).isEqualTo("OrderPlaced");
        assertThat((String) JsonPath.read(placed, "$.eventId")).isNotBlank();
        assertThat((Integer) JsonPath.read(placed, "$.version")).isEqualTo(1);
        assertThat((String) JsonPath.read(placed, "$.occurredAt")).isEqualTo("2026-10-14T03:15:00Z");
        assertThat((String) JsonPath.read(placed, "$.orderId")).isEqualTo("order-1");
        assertThat((String) JsonPath.read(placed, "$.userId")).isEqualTo("user-1");
        assertThat((List<String>) JsonPath.read(placed, "$.items[*].productId")).containsExactly("cpu", "ram");
        assertThat((List<Integer>) JsonPath.read(placed, "$.items[*].quantity")).containsExactly(1, 2);

        String cancelled = messages.getAllValues().get(1);
        assertThat((String) JsonPath.read(cancelled, "$.eventType")).isEqualTo("OrderCancelled");
        assertThat((String) JsonPath.read(cancelled, "$.orderId")).isEqualTo("order-1");
        assertThat((String) JsonPath.read(cancelled, "$.reason")).isEqualTo("PAYMENT_TIMEOUT");

        String completed = messages.getAllValues().get(2);
        assertThat((String) JsonPath.read(completed, "$.eventType")).isEqualTo("OrderCompleted");
        assertThat((String) JsonPath.read(completed, "$.orderId")).isEqualTo("order-1");
        // every event has its own id
        assertThat(messages.getAllValues()).extracting(message -> (String) JsonPath.read(message, "$.eventId"))
                .doesNotHaveDuplicates();
    }

    @Test
    @SuppressWarnings("unchecked")
    void aFailedSendDoesNotFailTheRequest() {
        KafkaOperations<String, String> kafka = mock(KafkaOperations.class);
        given(kafka.send(anyString(), anyString(), anyString())).willThrow(new IllegalStateException("broker down"));
        OrderEventKafkaPublisher publisher = new OrderEventKafkaPublisher(kafka, new ObjectMapper());

        publisher.on(new OrderCompleted("order-1", "user-1", OCCURRED_AT));
    }

    /** A message as Inventory Service publishes it: the common fields plus the event's own fields. */
    private static String reservationEvent(String eventType, String orderId, String extraFields) {
        return "{\"eventId\": \"" + UUID.randomUUID() + "\", \"eventType\": \"" + eventType
                + "\", \"version\": 1, \"occurredAt\": \"2026-10-14T03:15:00Z\", \"orderId\": \"" + orderId
                + "\", " + extraFields + "}";
    }
}
