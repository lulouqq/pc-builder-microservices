package com.csci318.inventoryservice;

import com.csci318.inventoryservice.domain.StockAlertType;
import com.csci318.inventoryservice.infrastructure.OrderEventKafkaListener;
import com.csci318.inventoryservice.infrastructure.StockAlertRepository;
import com.csci318.inventoryservice.service.StockAlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * I2: stock alerts for subscribed customers through the REST API, with Kafka messaging disabled, so the
 * alerts are created in-process (Architecture Decision 17). The Kafka path is tested in
 * InventoryKafkaIntegrationTests. Each test uses its own products and users (default threshold: 10).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StockAlertApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StockAlertRepository alerts;

    @Autowired
    private StockAlertService stockAlertService;

    @Autowired
    private OrderEventKafkaListener orderEventListener;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    private String productId;
    private String userId;
    private String customer;
    private String admin;

    @BeforeEach
    void setUp() {
        productId = "product-" + UUID.randomUUID();
        userId = "user-" + UUID.randomUUID();
        customer = token(userId);
        admin = TestTokens.bearer(jwtSecret, "admin-1", "ADMIN");
    }

    @Test
    void subscriberGetsAStockLowAlert() throws Exception {
        updateStock(productId, 12);
        subscribe(customer, productId, userId);

        // available stock 12 -> 5: falls below the threshold
        updateStock(productId, 5);

        getAlerts(customer, userId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").isNotEmpty())
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[0].productId").value(productId))
                .andExpect(jsonPath("$[0].type").value("STOCK_LOW"))
                .andExpect(jsonPath("$[0].availableQuantity").value(5))
                .andExpect(jsonPath("$[0].message").value("Product " + productId + " is running low: 5 left in stock"))
                .andExpect(jsonPath("$[0].occurredAt").isNotEmpty())
                .andExpect(jsonPath("$[0].sourceEventId").doesNotExist());
    }

    @Test
    void subscriberGetsAProductRestockedAlert() throws Exception {
        updateStock(productId, 0);
        subscribe(customer, productId, userId);

        updateStock(productId, 20);

        getAlerts(customer, userId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("PRODUCT_RESTOCKED"))
                .andExpect(jsonPath("$[0].availableQuantity").value(20))
                .andExpect(jsonPath("$[0].message").value("Product " + productId + " is back in stock: 20 available"));
    }

    @Test
    void onlyThresholdCrossingsAndRestocksCreateAlertsAndTheSubscriptionStaysActive() throws Exception {
        updateStock(productId, 12);
        subscribe(customer, productId, userId);

        updateStock(productId, 15);  // stays above the threshold: no alert
        updateStock(productId, 5);   // STOCK_LOW
        updateStock(productId, 3);   // already below: no second STOCK_LOW
        updateStock(productId, 0);   // no alert
        updateStock(productId, 30);  // PRODUCT_RESTOCKED
        updateStock(productId, 8);   // STOCK_LOW again: the subscription is still active

        // newest first
        getAlerts(customer, userId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].type").value("STOCK_LOW"))
                .andExpect(jsonPath("$[0].availableQuantity").value(8))
                .andExpect(jsonPath("$[1].type").value("PRODUCT_RESTOCKED"))
                .andExpect(jsonPath("$[1].availableQuantity").value(30))
                .andExpect(jsonPath("$[2].type").value("STOCK_LOW"))
                .andExpect(jsonPath("$[2].availableQuantity").value(5));
    }

    @Test
    void everySubscriberOfTheProductIsNotifiedAndNobodyElse() throws Exception {
        String otherProduct = "product-" + UUID.randomUUID();
        String secondUser = "user-" + UUID.randomUUID();
        String otherProductUser = "user-" + UUID.randomUUID();
        String notSubscribedUser = "user-" + UUID.randomUUID();
        updateStock(productId, 12);
        updateStock(otherProduct, 12);
        subscribe(customer, productId, userId);
        subscribe(token(secondUser), productId, secondUser);
        subscribe(token(otherProductUser), otherProduct, otherProductUser);

        updateStock(productId, 5);

        for (String subscriber : List.of(userId, secondUser)) {
            getAlerts(token(subscriber), subscriber)
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].userId").value(subscriber))
                    .andExpect(jsonPath("$[0].productId").value(productId));
        }
        for (String other : List.of(otherProductUser, notSubscribedUser)) {
            getAlerts(token(other), other)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Test
    void anOrderReservationThatCrossesTheThresholdCreatesAStockLowAlert() throws Exception {
        updateStock(productId, 12);
        subscribe(customer, productId, userId);

        // OrderPlaced as consumed from order-events: available stock 12 -> 9
        String orderId = "order-" + UUID.randomUUID();
        orderEventListener.onMessage("{\"eventId\": \"" + UUID.randomUUID() + "\", \"eventType\": \"OrderPlaced\", "
                + "\"version\": 1, \"occurredAt\": \"2026-10-14T03:15:00Z\", \"orderId\": \"" + orderId + "\", "
                + "\"userId\": \"user-1\", \"items\": [{\"productId\": \"" + productId + "\", \"quantity\": 3}]}");

        getAlerts(customer, userId)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("STOCK_LOW"))
                .andExpect(jsonPath("$[0].availableQuantity").value(9));
    }

    @Test
    void aRepeatedSourceEventCreatesOneAlertPerSubscriber() throws Exception {
        updateStock(productId, 12);
        subscribe(customer, productId, userId);
        Instant occurredAt = Instant.parse("2026-10-14T03:15:00Z");

        assertThat(stockAlertService.notifySubscribers("event-" + productId, productId, StockAlertType.STOCK_LOW,
                4, occurredAt)).isEqualTo(1);
        assertThat(stockAlertService.notifySubscribers("event-" + productId, productId, StockAlertType.STOCK_LOW,
                4, occurredAt)).isZero();

        assertThat(alerts.findByUserIdOrderByOccurredAtDesc(userId)).singleElement().satisfies(alert -> {
            assertThat(alert.getSourceEventId()).isEqualTo("event-" + productId);
            assertThat(alert.getOccurredAt()).isEqualTo(occurredAt);
        });
    }

    @Test
    void noSubscribersNoAlerts() throws Exception {
        long before = alerts.count();

        updateStock(productId, 12);
        updateStock(productId, 5);

        assertThat(alerts.count()).isEqualTo(before);
    }

    // --- access control ---

    @Test
    void alertsAreOnlyForTheirOwner() throws Exception {
        String expired = TestTokens.bearer(jwtSecret, userId, "CUSTOMER", Instant.now().minusSeconds(60));
        String foreign = TestTokens.bearer("another-secret-that-is-long-enough-123", userId, "CUSTOMER");

        mockMvc.perform(get("/users/{userId}/stock-alerts", userId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AuthenticationRequired"));
        for (String badToken : List.of(expired, foreign, "Bearer not-a-token")) {
            getAlerts(badToken, userId)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AuthenticationRequired"));
        }
        // another customer, and an admin reading a customer's alerts (ADMIN does not bypass the owner check)
        getAlerts(token("user-" + UUID.randomUUID()), userId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AccessDenied"));
        getAlerts(admin, userId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AccessDenied"));
        // the owner, even without any alerts
        getAlerts(customer, userId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private String token(String userId) {
        return TestTokens.bearer(jwtSecret, userId, "CUSTOMER");
    }

    private void updateStock(String productId, int quantity) throws Exception {
        mockMvc.perform(put("/inventory/{id}", productId)
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": " + quantity + "}"))
                .andExpect(status().isOk());
    }

    private void subscribe(String token, String productId, String userId) throws Exception {
        mockMvc.perform(post("/inventory/{id}/notifications", productId)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": \"" + userId + "\"}"))
                .andExpect(status().isCreated());
    }

    private ResultActions getAlerts(String token, String userId) throws Exception {
        return mockMvc.perform(get("/users/{userId}/stock-alerts", userId)
                .header(HttpHeaders.AUTHORIZATION, token));
    }
}
