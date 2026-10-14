package com.csci318.inventoryservice;

import com.csci318.inventoryservice.domain.InventoryItem;
import com.csci318.inventoryservice.domain.ReservationStatus;
import com.csci318.inventoryservice.domain.event.ProductRestocked;
import com.csci318.inventoryservice.domain.event.StockLow;
import com.csci318.inventoryservice.domain.event.StockNotificationSubscribed;
import com.csci318.inventoryservice.infrastructure.InventoryEventStore;
import com.csci318.inventoryservice.infrastructure.StockNotificationSubscriptionRepository;
import com.csci318.inventoryservice.infrastructure.StockReservationRepository;
import com.csci318.inventoryservice.infrastructure.StoredEvent;
import com.csci318.inventoryservice.infrastructure.StoredEventRepository;
import com.csci318.inventoryservice.service.ConfirmStockReservationCommand;
import com.csci318.inventoryservice.service.InventoryCommandService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * I1 (REST), I2 (subscription) and I3 through the REST API, including the event store, the read
 * model and the derived events. Each test uses its own product id.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RecordApplicationEvents
class InventoryApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StoredEventRepository storedEvents;

    @Autowired
    private InventoryEventStore eventStore;

    @Autowired
    private StockReservationRepository reservations;

    @Autowired
    private StockNotificationSubscriptionRepository subscriptions;

    @Autowired
    private InventoryCommandService commandService;

    @Autowired
    private ApplicationEvents events;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    private String productId;
    private String admin;
    private String customer;

    @BeforeEach
    void setUp() {
        productId = "product-" + UUID.randomUUID();
        admin = TestTokens.bearer(jwtSecret, "admin-1", "ADMIN");
        customer = TestTokens.bearer(jwtSecret, "user-1", "CUSTOMER");
    }

    // --- I3: update and retrieve stock ---

    @Test
    void firstStockUpdateCreatesTheInventoryRecord() throws Exception {
        mockMvc.perform(get("/inventory/{id}", productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ResourceNotFound"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        updateStock(25, "initial delivery")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.quantity").value(25))
                .andExpect(jsonPath("$.reservedQuantity").value(0));

        // public read, no token
        mockMvc.perform(get("/inventory/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.quantity").value(25))
                .andExpect(jsonPath("$.reservedQuantity").value(0));

        List<StoredEvent> stream = stream();
        assertThat(stream).extracting(StoredEvent::getEventType)
                .containsExactly("InventoryInitialized", "StockLevelUpdated");
        assertThat(stream).extracting(StoredEvent::getAggregateVersion).containsExactly(1L, 2L);
        assertThat(stream).allSatisfy(event -> {
            assertThat(event.getAggregateId()).isEqualTo("inventory-item-" + productId);
            assertThat(event.getAggregateType()).isEqualTo("InventoryItem");
            assertThat(event.getEventId()).isNotBlank();
            assertThat(event.getOccurredAt()).isNotNull();
        });
        assertThat((String) JsonPath.read(stream.get(1).getPayload(), "$.reason")).isEqualTo("initial delivery");
        assertThat((Integer) JsonPath.read(stream.get(1).getPayload(), "$.previousQuantity")).isZero();
        assertThat((Integer) JsonPath.read(stream.get(1).getPayload(), "$.newQuantity")).isEqualTo(25);
    }

    @Test
    void stockUpdateSetsAbsoluteQuantityAndReasonIsOptional() throws Exception {
        updateStock(25, null).andExpect(status().isOk());

        updateStock(7, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(7));

        assertThat(stream()).extracting(StoredEvent::getEventType)
                .containsExactly("InventoryInitialized", "StockLevelUpdated", "StockLevelUpdated");
    }

    @Test
    void rejectsInvalidStockUpdates() throws Exception {
        for (String body : List.of("{\"quantity\": -1}", "{}", "{\"quantity\": \"many\"}", "not json")) {
            mockMvc.perform(put("/inventory/{id}", productId)
                            .header(HttpHeaders.AUTHORIZATION, admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("ValidationFailed"));
        }
        // nothing was recorded, so the product still has no inventory record
        assertThat(stream()).isEmpty();
        mockMvc.perform(get("/inventory/{id}", productId)).andExpect(status().isNotFound());
    }

    @Test
    void rejectsStockUpdateBelowReservedQuantity() throws Exception {
        updateStock(10, null);
        reserve("order-1", 6).andExpect(status().isCreated());

        updateStock(5, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ValidationFailed"));

        mockMvc.perform(get("/inventory/{id}", productId))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.reservedQuantity").value(6));
        updateStock(6, null).andExpect(status().isOk());
    }

    // --- I1: reserve and release stock (REST) ---

    @Test
    void reservesAndReleasesStock() throws Exception {
        updateStock(10, null);

        String body = reserve("order-1", 4)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.orderId").value("order-1"))
                .andExpect(jsonPath("$.quantity").value(4))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        String reservationId = JsonPath.read(body, "$.id");

        mockMvc.perform(get("/inventory/{id}", productId))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.reservedQuantity").value(4));

        mockMvc.perform(delete("/inventory/{id}/reservations/{reservationId}", productId, reservationId)
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/inventory/{id}", productId))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.reservedQuantity").value(0));
        assertThat(reservations.findById(reservationId).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.RELEASED);
        assertThat(stream()).extracting(StoredEvent::getEventType).containsExactly(
                "InventoryInitialized", "StockLevelUpdated", "StockReserved", "StockReservationReleased");
    }

    @Test
    void cannotReserveMoreThanAvailable() throws Exception {
        updateStock(5, null);
        reserve("order-1", 4).andExpect(status().isCreated());

        reserve("order-2", 2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("StockUnavailable"));

        mockMvc.perform(get("/inventory/{id}", productId))
                .andExpect(jsonPath("$.reservedQuantity").value(4));
    }

    @Test
    void rejectsInvalidReservationRequests() throws Exception {
        updateStock(5, null);

        for (String body : List.of("{\"orderId\": \"order-1\", \"quantity\": 0}", "{\"quantity\": 1}",
                "{\"orderId\": \"order-1\"}")) {
            mockMvc.perform(post("/inventory/{id}/reservations", productId)
                            .header(HttpHeaders.AUTHORIZATION, admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("ValidationFailed"));
        }
    }

    @Test
    void releasingAnUnknownOrReleasedReservationIsAConflict() throws Exception {
        updateStock(5, null);
        String reservationId = JsonPath.read(
                reserve("order-1", 1).andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(delete("/inventory/{id}/reservations/{reservationId}", productId, "unknown")
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ReservationConflict"));

        mockMvc.perform(delete("/inventory/{id}/reservations/{reservationId}", productId, reservationId)
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/inventory/{id}/reservations/{reservationId}", productId, reservationId)
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ReservationConflict"));
    }

    @Test
    void reservationEndpointsReturnNotFoundWithoutInventoryRecord() throws Exception {
        reserve("order-1", 1)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ResourceNotFound"));
        mockMvc.perform(delete("/inventory/{id}/reservations/{reservationId}", productId, "r1")
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void confirmingAReservationDeductsTheStock() throws Exception {
        updateStock(10, null);
        String reservationId = JsonPath.read(
                reserve("order-1", 4).andReturn().getResponse().getContentAsString(), "$.id");

        // no REST endpoint: ConfirmStockReservationCommand is issued when OrderCompleted is consumed
        commandService.handle(new ConfirmStockReservationCommand(productId, reservationId));

        mockMvc.perform(get("/inventory/{id}", productId))
                .andExpect(jsonPath("$.quantity").value(6))
                .andExpect(jsonPath("$.reservedQuantity").value(0));
        assertThat(reservations.findById(reservationId).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void readModelMatchesTheStateRebuiltFromTheEventStore() throws Exception {
        updateStock(20, null);
        String first = JsonPath.read(reserve("order-1", 5).andReturn().getResponse().getContentAsString(), "$.id");
        String second = JsonPath.read(reserve("order-2", 3).andReturn().getResponse().getContentAsString(), "$.id");
        reserve("order-3", 2);
        commandService.handle(new ConfirmStockReservationCommand(productId, first));
        mockMvc.perform(delete("/inventory/{id}/reservations/{reservationId}", productId, second)
                .header(HttpHeaders.AUTHORIZATION, admin));
        updateStock(30, "delivery");

        InventoryItem replayed = eventStore.load(InventoryItem.idFor(productId)).orElseThrow();

        assertThat(replayed.getQuantity()).isEqualTo(30);
        assertThat(replayed.getReservedQuantity()).isEqualTo(2);
        assertThat(replayed.getVersion()).isEqualTo(8);
        mockMvc.perform(get("/inventory/{id}", productId))
                .andExpect(jsonPath("$.quantity").value(replayed.getQuantity()))
                .andExpect(jsonPath("$.reservedQuantity").value(replayed.getReservedQuantity()));
    }

    // --- derived events (default threshold: 10) ---

    @Test
    void publishesProductRestockedAndStockLow() throws Exception {
        updateStock(12, null);
        assertThat(events.stream(ProductRestocked.class)).extracting(ProductRestocked::productId)
                .containsExactly(productId);
        assertThat(events.stream(StockLow.class)).isEmpty();

        // available stock 12 -> 9: falls below the threshold
        reserve("order-1", 3).andExpect(status().isCreated());
        assertThat(events.stream(StockLow.class)).singleElement().satisfies(stockLow -> {
            assertThat(stockLow.productId()).isEqualTo(productId);
            assertThat(stockLow.availableQuantity()).isEqualTo(9);
            assertThat(stockLow.threshold()).isEqualTo(10);
        });

        // already below the threshold: no second StockLow
        reserve("order-2", 1).andExpect(status().isCreated());
        assertThat(events.stream(StockLow.class)).hasSize(1);
        assertThat(events.stream(ProductRestocked.class)).hasSize(1);
    }

    // --- I2: stock notification subscription ---

    @Test
    void customerSubscribesToStockNotifications() throws Exception {
        updateStock(0, null);

        String body = subscribe(customer, "user-1")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String subscriptionId = JsonPath.read(body, "$.id");

        assertThat(subscriptions.findById(subscriptionId)).isPresent();
        assertThat(events.stream(StockNotificationSubscribed.class))
                .extracting(StockNotificationSubscribed::subscriptionId).containsExactly(subscriptionId);

        // subscribing again returns the same subscription and raises no second event
        subscribe(customer, "user-1")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(subscriptionId));
        assertThat(subscriptions.findByProductIdAndUserId(productId, "user-1")).isPresent();
        assertThat(events.stream(StockNotificationSubscribed.class)).hasSize(1);
    }

    @Test
    void subscriptionRequiresTheOwnersToken() throws Exception {
        updateStock(0, null);

        mockMvc.perform(post("/inventory/{id}/notifications", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": \"user-1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AuthenticationRequired"));
        // another user's id, and an admin acting for a customer
        subscribe(customer, "user-2")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AccessDenied"));
        subscribe(admin, "user-1")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AccessDenied"));

        assertThat(subscriptions.findByProductIdAndUserId(productId, "user-1")).isEmpty();
        assertThat(subscriptions.findByProductIdAndUserId(productId, "user-2")).isEmpty();
    }

    @Test
    void subscriptionValidatesBodyAndProduct() throws Exception {
        mockMvc.perform(post("/inventory/{id}/notifications", productId)
                        .header(HttpHeaders.AUTHORIZATION, customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ValidationFailed"));
        // no inventory record for the product
        subscribe(customer, "user-1")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ResourceNotFound"));
    }

    // --- access control ---

    @Test
    void adminEndpointsRejectMissingInvalidAndCustomerTokens() throws Exception {
        String expired = TestTokens.bearer(jwtSecret, "admin-1", "ADMIN", java.time.Instant.now().minusSeconds(60));
        String foreign = TestTokens.bearer("another-secret-that-is-long-enough-123", "admin-1", "ADMIN");

        mockMvc.perform(put("/inventory/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\": 5}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AuthenticationRequired"))
                .andExpect(jsonPath("$.message").isNotEmpty());
        for (String badToken : List.of(expired, foreign, "Bearer not-a-token")) {
            mockMvc.perform(put("/inventory/{id}", productId)
                            .header(HttpHeaders.AUTHORIZATION, badToken)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\": 5}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AuthenticationRequired"));
        }

        mockMvc.perform(put("/inventory/{id}", productId)
                        .header(HttpHeaders.AUTHORIZATION, customer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\": 5}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AccessDenied"));
        mockMvc.perform(post("/inventory/{id}/reservations", productId)
                        .header(HttpHeaders.AUTHORIZATION, customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": \"order-1\", \"quantity\": 1}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/inventory/{id}/reservations/{reservationId}", productId, "r1")
                        .header(HttpHeaders.AUTHORIZATION, customer))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/inventory/{id}/reservations", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": \"order-1\", \"quantity\": 1}"))
                .andExpect(status().isUnauthorized());

        assertThat(stream()).isEmpty();
    }

    private ResultActions updateStock(int quantity, String reason) throws Exception {
        String body = reason == null
                ? "{\"quantity\": " + quantity + "}"
                : "{\"quantity\": " + quantity + ", \"reason\": \"" + reason + "\"}";
        return mockMvc.perform(put("/inventory/{id}", productId)
                .header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions reserve(String orderId, int quantity) throws Exception {
        return mockMvc.perform(post("/inventory/{id}/reservations", productId)
                .header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\": \"" + orderId + "\", \"quantity\": " + quantity + "}"));
    }

    private ResultActions subscribe(String token, String userId) throws Exception {
        return mockMvc.perform(post("/inventory/{id}/notifications", productId)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\": \"" + userId + "\"}"));
    }

    private List<StoredEvent> stream() {
        return storedEvents.findByAggregateIdOrderByAggregateVersionAsc(InventoryItem.idFor(productId));
    }
}
