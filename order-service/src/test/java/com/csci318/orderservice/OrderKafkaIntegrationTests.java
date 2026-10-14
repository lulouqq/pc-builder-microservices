package com.csci318.orderservice;

import com.csci318.orderservice.domain.OrderStatus;
import com.csci318.orderservice.domain.PaymentStatus;
import com.csci318.orderservice.service.AccountClient;
import com.csci318.orderservice.service.CatalogClient;
import com.csci318.orderservice.service.CatalogProduct;
import com.csci318.orderservice.service.OrderService;
import com.csci318.orderservice.service.RecommendationClient;
import com.jayway.jsonpath.JsonPath;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.given;

/**
 * The order flow through a real (embedded) Kafka broker: Order Service publishes order-events and
 * consumes inventory-reservation-events. Inventory Service is played by the test.
 */
@SpringBootTest(properties = {
        "messaging.kafka.enabled=true",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"})
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"order-events", "inventory-reservation-events"})
@DirtiesContext
class OrderKafkaIntegrationTests {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private OrderService orderService;

    @MockBean
    private CatalogClient catalogClient;

    @MockBean
    private AccountClient accountClient;

    @MockBean
    private RecommendationClient recommendationClient;

    private Consumer<String, String> orderEvents;
    private String userId;
    private String orderId;

    @BeforeEach
    void setUp() {
        orderEvents = new DefaultKafkaConsumerFactory<>(
                KafkaTestUtils.consumerProps("test-" + UUID.randomUUID(), "true", broker),
                new StringDeserializer(), new StringDeserializer()).createConsumer();
        broker.consumeFromAnEmbeddedTopic(orderEvents, "order-events");

        userId = "user-" + UUID.randomUUID();
        given(catalogClient.findProduct("cpu")).willReturn(Optional.of(new CatalogProduct("cpu", "Ryzen 5", 299f)));
        given(accountClient.getSavedAddressIds(userId, "Bearer token")).willReturn(List.of("address-1"));
        orderId = orderService.createDraft(userId, userId).getId();
        orderService.addItem(orderId, "cpu", 2, userId);
        orderService.selectDeliveryAddress(orderId, "address-1", userId, "Bearer token");
    }

    @AfterEach
    void closeConsumer() {
        orderEvents.close();
    }

    @Test
    void placedPaidOrderGoesThroughKafka() {
        orderService.checkout(orderId, userId);

        ConsumerRecord<String, String> placed = next("OrderPlaced");
        assertThat(placed.key()).isEqualTo(orderId);
        assertThat((String) JsonPath.read(placed.value(), "$.userId")).isEqualTo(userId);
        assertThat((Integer) JsonPath.read(placed.value(), "$.version")).isEqualTo(1);
        assertThat((List<String>) JsonPath.read(placed.value(), "$.items[*].productId")).containsExactly("cpu");
        assertThat((List<Integer>) JsonPath.read(placed.value(), "$.items[*].quantity")).containsExactly(2);

        // Inventory Service answers with StockReserved
        sendReservationEvent("StockReserved",
                "\"reservations\": [{\"reservationId\": \"r1\", \"productId\": \"cpu\", \"quantity\": 2}]");
        await().atMost(TIMEOUT).untilAsserted(() ->
                assertThat(orderService.getOrder(orderId, userId).getStatus()).isEqualTo(OrderStatus.PROCESSING));

        orderService.recordPayment(orderId, PaymentStatus.PAID, userId);

        ConsumerRecord<String, String> completed = next("OrderCompleted");
        assertThat(completed.key()).isEqualTo(orderId);
        assertThat((String) JsonPath.read(completed.value(), "$.orderId")).isEqualTo(orderId);
    }

    @Test
    void failedReservationCancelsTheOrderAndCustomerCancelIsPublished() {
        orderService.checkout(orderId, userId);
        next("OrderPlaced");

        sendReservationEvent("StockReservationFailed", "\"reason\": \"STOCK_UNAVAILABLE\", \"unavailableItems\": []");
        await().atMost(TIMEOUT).untilAsserted(() ->
                assertThat(orderService.getOrder(orderId, userId).getStatus()).isEqualTo(OrderStatus.CANCELLED));

        // a second order of the same customer, cancelled by the customer after checkout
        String secondOrder = orderService.createDraft(userId, userId).getId();
        orderService.addItem(secondOrder, "cpu", 1, userId);
        orderService.selectDeliveryAddress(secondOrder, "address-1", userId, "Bearer token");
        orderService.checkout(secondOrder, userId);
        orderService.cancel(secondOrder, userId);

        orderId = secondOrder;
        next("OrderPlaced");
        ConsumerRecord<String, String> cancelled = next("OrderCancelled");
        assertThat(cancelled.key()).isEqualTo(secondOrder);
        assertThat((String) JsonPath.read(cancelled.value(), "$.reason")).isEqualTo("CUSTOMER_CANCELLED");
    }

    private void sendReservationEvent(String eventType, String extraFields) {
        kafkaTemplate.send("inventory-reservation-events", orderId, "{\"eventId\": \"" + UUID.randomUUID()
                + "\", \"eventType\": \"" + eventType + "\", \"version\": 1, "
                + "\"occurredAt\": \"2026-10-14T03:15:00Z\", \"orderId\": \"" + orderId + "\", " + extraFields + "}");
    }

    /** The next event of the given type for this test's current order. */
    private ConsumerRecord<String, String> next(String eventType) {
        long deadline = System.currentTimeMillis() + TIMEOUT.toMillis();
        while (System.currentTimeMillis() < deadline) {
            for (ConsumerRecord<String, String> record : orderEvents.poll(Duration.ofMillis(200))) {
                if (orderId.equals(record.key())
                        && eventType.equals(JsonPath.read(record.value(), "$.eventType"))) {
                    return record;
                }
            }
        }
        throw new AssertionError("No " + eventType + " received for order " + orderId + " within " + TIMEOUT);
    }
}
