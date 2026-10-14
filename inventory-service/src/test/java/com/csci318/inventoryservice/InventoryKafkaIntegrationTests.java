package com.csci318.inventoryservice;

import com.csci318.inventoryservice.domain.StockAlertType;
import com.csci318.inventoryservice.infrastructure.InventoryReadModelRepository;
import com.csci318.inventoryservice.infrastructure.StockAlertRepository;
import com.csci318.inventoryservice.service.InventoryCommandService;
import com.csci318.inventoryservice.service.SubscribeStockNotificationCommand;
import com.csci318.inventoryservice.service.UpdateStockLevelCommand;
import com.jayway.jsonpath.JsonPath;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * The order flow through a real (embedded) Kafka broker: Inventory Service consumes order-events
 * and publishes inventory-reservation-events and inventory-stock-events. It also consumes its own
 * inventory-stock-events to create stock alerts (I2).
 */
@SpringBootTest(properties = {
        "messaging.kafka.enabled=true",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"})
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1,
        topics = {"order-events", "inventory-reservation-events", "inventory-stock-events"})
@DirtiesContext
class InventoryKafkaIntegrationTests {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private InventoryCommandService commandService;

    @Autowired
    private InventoryReadModelRepository readModels;

    @Autowired
    private StockAlertRepository alerts;

    private Consumer<String, String> reservationEvents;
    private Consumer<String, String> stockEvents;
    private String productId;
    private String orderId;

    @BeforeEach
    void setUp() {
        reservationEvents = consumer("inventory-reservation-events");
        stockEvents = consumer("inventory-stock-events");
        productId = "product-" + UUID.randomUUID();
        orderId = "order-" + UUID.randomUUID();
    }

    @AfterEach
    void closeConsumers() {
        reservationEvents.close();
        stockEvents.close();
    }

    @Test
    void reservesReleasesAndReportsThroughKafka() {
        commandService.handle(new UpdateStockLevelCommand(productId, 5, null));

        // the stock update is published as StockChanged, keyed by the product
        ConsumerRecord<String, String> stockChanged = next(stockEvents, productId);
        assertThat(stockChanged.key()).isEqualTo(productId);
        assertThat((String) JsonPath.read(stockChanged.value(), "$.eventType")).isEqualTo("StockChanged");
        assertThat((Integer) JsonPath.read(stockChanged.value(), "$.quantity")).isEqualTo(5);
        assertThat((Integer) JsonPath.read(stockChanged.value(), "$.reservedQuantity")).isZero();
        assertThat((String) JsonPath.read(stockChanged.value(), "$.change")).isIn(
                "STOCK_LEVEL_UPDATED", "PRODUCT_RESTOCKED");

        // OrderPlaced -> StockReserved, keyed by the order
        send(orderId, "OrderPlaced",
                "\"userId\": \"user-1\", \"items\": [{\"productId\": \"" + productId + "\", \"quantity\": 2}]");
        ConsumerRecord<String, String> reserved = next(reservationEvents, orderId);
        assertThat(reserved.key()).isEqualTo(orderId);
        assertThat((String) JsonPath.read(reserved.value(), "$.eventType")).isEqualTo("StockReserved");
        assertThat((Integer) JsonPath.read(reserved.value(), "$.version")).isEqualTo(1);
        assertThat((String) JsonPath.read(reserved.value(), "$.eventId")).isNotBlank();
        assertThat((String) JsonPath.read(reserved.value(), "$.occurredAt")).isNotBlank();
        assertThat((String) JsonPath.read(reserved.value(), "$.orderId")).isEqualTo(orderId);
        assertThat((List<String>) JsonPath.read(reserved.value(), "$.reservations[*].productId"))
                .containsExactly(productId);
        assertThat((List<Integer>) JsonPath.read(reserved.value(), "$.reservations[*].quantity"))
                .containsExactly(2);
        assertThat((String) JsonPath.read(reserved.value(), "$.reservations[0].reservationId")).isNotBlank();
        assertThat(readModels.findById(productId).orElseThrow().getReservedQuantity()).isEqualTo(2);

        // a second order asks for more than is left -> StockReservationFailed
        String secondOrder = "order-" + UUID.randomUUID();
        send(secondOrder, "OrderPlaced",
                "\"userId\": \"user-2\", \"items\": [{\"productId\": \"" + productId + "\", \"quantity\": 4}]");
        ConsumerRecord<String, String> failed = next(reservationEvents, secondOrder);
        assertThat(failed.key()).isEqualTo(secondOrder);
        assertThat((String) JsonPath.read(failed.value(), "$.eventType")).isEqualTo("StockReservationFailed");
        assertThat((String) JsonPath.read(failed.value(), "$.reason")).isEqualTo("STOCK_UNAVAILABLE");
        assertThat((Integer) JsonPath.read(failed.value(), "$.unavailableItems[0].requestedQuantity")).isEqualTo(4);
        assertThat((Integer) JsonPath.read(failed.value(), "$.unavailableItems[0].availableQuantity")).isEqualTo(3);

        // OrderCancelled releases the first order's reservation
        send(orderId, "OrderCancelled", "\"userId\": \"user-1\", \"reason\": \"CUSTOMER_CANCELLED\"");
        await().atMost(TIMEOUT).untilAsserted(() ->
                assertThat(readModels.findById(productId).orElseThrow().getReservedQuantity()).isZero());
        assertThat(readModels.findById(productId).orElseThrow().getQuantity()).isEqualTo(5);
    }

    @Test
    void orderCompletedConfirmsTheReservationThroughKafka() {
        commandService.handle(new UpdateStockLevelCommand(productId, 5, null));
        send(orderId, "OrderPlaced",
                "\"userId\": \"user-1\", \"items\": [{\"productId\": \"" + productId + "\", \"quantity\": 2}]");
        send(orderId, "OrderCompleted", "\"userId\": \"user-1\"");

        await().atMost(TIMEOUT).untilAsserted(() -> {
            assertThat(readModels.findById(productId).orElseThrow().getQuantity()).isEqualTo(3);
            assertThat(readModels.findById(productId).orElseThrow().getReservedQuantity()).isZero();
        });
    }

    @Test
    void aRestockGoesThroughKafkaToTheSubscribersStockAlert() {
        String userId = "user-" + UUID.randomUUID();
        commandService.handle(new UpdateStockLevelCommand(productId, 0, null));
        commandService.handle(new SubscribeStockNotificationCommand(productId, userId));

        // available stock 0 -> 5: ProductRestocked -> StockChanged (PRODUCT_RESTOCKED) -> StockAlert
        commandService.handle(new UpdateStockLevelCommand(productId, 5, null));

        await().atMost(TIMEOUT).untilAsserted(() ->
                assertThat(alerts.findByUserIdOrderByOccurredAtDesc(userId)).singleElement().satisfies(alert -> {
                    assertThat(alert.getProductId()).isEqualTo(productId);
                    assertThat(alert.getType()).isEqualTo(StockAlertType.PRODUCT_RESTOCKED);
                    assertThat(alert.getAvailableQuantity()).isEqualTo(5);
                    assertThat(alert.getSourceEventId()).isNotBlank();
                }));
    }

    @Test
    void aRepeatedStockChangedCreatesOneAlert() {
        String userId = "user-" + UUID.randomUUID();
        commandService.handle(new UpdateStockLevelCommand(productId, 0, null));
        commandService.handle(new SubscribeStockNotificationCommand(productId, userId));
        String eventId = UUID.randomUUID().toString();
        String message = "{\"eventId\": \"" + eventId + "\", \"eventType\": \"StockChanged\", \"version\": 1, "
                + "\"occurredAt\": \"2026-10-14T03:15:00Z\", \"productId\": \"" + productId + "\", "
                + "\"quantity\": 4, \"reservedQuantity\": 0, \"change\": \"STOCK_LOW\"}";

        kafkaTemplate.send("inventory-stock-events", productId, message);
        kafkaTemplate.send("inventory-stock-events", productId, message);
        String marker = UUID.randomUUID().toString();
        kafkaTemplate.send("inventory-stock-events", productId, message.replace(eventId, marker));

        // the third message is consumed after the first two (same key, same partition)
        await().atMost(TIMEOUT).until(() -> alerts.existsBySourceEventIdAndUserId(marker, userId));
        assertThat(alerts.existsBySourceEventIdAndUserId(eventId, userId)).isTrue();
        assertThat(alerts.findByUserIdOrderByOccurredAtDesc(userId)).hasSize(2);
    }

    private void send(String orderId, String eventType, String extraFields) {
        kafkaTemplate.send("order-events", orderId, "{\"eventId\": \"" + UUID.randomUUID()
                + "\", \"eventType\": \"" + eventType + "\", \"version\": 1, "
                + "\"occurredAt\": \"2026-10-14T03:15:00Z\", \"orderId\": \"" + orderId + "\", " + extraFields + "}");
    }

    /** The next message with the given key (other tests may share the topics). */
    private ConsumerRecord<String, String> next(Consumer<String, String> consumer, String key) {
        long deadline = System.currentTimeMillis() + TIMEOUT.toMillis();
        while (System.currentTimeMillis() < deadline) {
            for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(200))) {
                if (key.equals(record.key())) {
                    return record;
                }
            }
        }
        throw new AssertionError("No Kafka message with key " + key + " received within " + TIMEOUT);
    }

    private Consumer<String, String> consumer(String topic) {
        Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                KafkaTestUtils.consumerProps("test-" + UUID.randomUUID(), "true", broker),
                new StringDeserializer(), new StringDeserializer()).createConsumer();
        broker.consumeFromAnEmbeddedTopic(consumer, topic);
        return consumer;
    }
}
