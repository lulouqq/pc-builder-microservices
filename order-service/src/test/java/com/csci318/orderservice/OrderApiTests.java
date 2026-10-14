package com.csci318.orderservice;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.csci318.orderservice.domain.event.OrderCancelled;
import com.csci318.orderservice.domain.event.OrderCompleted;
import com.csci318.orderservice.domain.event.OrderPlaced;
import com.csci318.orderservice.domain.event.PaymentStatusUpdated;
import com.csci318.orderservice.infrastructure.OrderRepository;
import com.csci318.orderservice.service.AccountClient;
import com.csci318.orderservice.service.AuthenticationRequiredException;
import com.csci318.orderservice.service.CatalogClient;
import com.csci318.orderservice.service.CatalogProduct;
import com.csci318.orderservice.service.OrderService;
import com.csci318.orderservice.service.RecommendationClient;
import com.csci318.orderservice.service.RecommendedBuild;
import com.csci318.orderservice.service.ResourceNotFoundException;
import com.csci318.orderservice.service.ServiceUnavailableException;
import com.jayway.jsonpath.JsonPath;

/**
 * The Order REST API (O1, O2, O4, O5, O6, R3) and the order lifecycle. Catalog, Account and
 * Recommendation Service are mocked; the reservation results that will arrive from Kafka are
 * simulated by calling OrderService directly. Each test uses its own user.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RecordApplicationEvents
class OrderApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ApplicationEvents events;

    @MockBean
    private CatalogClient catalogClient;

    @MockBean
    private AccountClient accountClient;

    @MockBean
    private RecommendationClient recommendationClient;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    private String userId;
    private String token;
    private String otherUserToken;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        
        userId = "user-" + UUID.randomUUID();
        token = TestTokens.bearer(jwtSecret, userId, "CUSTOMER");
        otherUserToken = TestTokens.bearer(jwtSecret, "someone-else", "CUSTOMER");
        given(catalogClient.findProduct(anyString())).willReturn(Optional.empty());
        given(catalogClient.findProduct("cpu")).willReturn(Optional.of(new CatalogProduct("cpu", "Ryzen 5", 299f)));
        given(catalogClient.findProduct("ram")).willReturn(Optional.of(new CatalogProduct("ram", "16GB", 80f)));
        given(accountClient.getSavedAddressIds(userId, token)).willReturn(List.of("address-1", "address-2"));
    }

    // --- O1: cart ---

    @Test
    void createsTheCart() throws Exception {
        createOrderRequest()
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.totalPrice").value(0.0))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void customerCanHaveOnlyOneCart() throws Exception {
        String orderId = createOrder();

        createOrderRequest()
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OrderStateViolation"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        // once the cart is cancelled a new one can be created
        perform(post("/orders/{id}/cancel", orderId), token).andExpect(status().isOk());
        createOrderRequest().andExpect(status().isCreated());
    }

    @Test
    void addsItemsAtTheCatalogPriceAndMergesQuantities() throws Exception {
        String orderId = createOrder();

        addItem(orderId, "cpu", 1)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").isNotEmpty())
                .andExpect(jsonPath("$.items[0].productId").value("cpu"))
                .andExpect(jsonPath("$.items[0].quantity").value(1))
                .andExpect(jsonPath("$.items[0].unitPrice").value(299.0))
                .andExpect(jsonPath("$.totalPrice").value(299.0));
        addItem(orderId, "ram", 2).andExpect(jsonPath("$.totalPrice").value(459.0));

        // the same product again: quantity is increased, no second line
        addItem(orderId, "ram", 1)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[?(@.productId == 'ram')].quantity").value(contains(3)))
                .andExpect(jsonPath("$.totalPrice").value(539.0));
    }

    @Test
    void rejectsInvalidItems() throws Exception {
        String orderId = createOrder();

        for (String body : List.of("{\"productId\": \"cpu\", \"quantity\": 0}", "{\"quantity\": 1}",
                "{\"productId\": \"cpu\"}", "not json")) {
            perform(post("/orders/{id}/items", orderId).contentType(MediaType.APPLICATION_JSON).content(body), token)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("ValidationFailed"));
        }
        addItem(orderId, "not-in-catalog", 1)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ResourceNotFound"));
        given(catalogClient.findProduct("cpu")).willThrow(new ServiceUnavailableException("Catalog down", null));
        addItem(orderId, "cpu", 1)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ServiceUnavailable"));

        getOrder(orderId).andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void removesItems() throws Exception {
        String orderId = createOrder();
        addItem(orderId, "cpu", 1);
        String body = addItem(orderId, "ram", 2).andReturn().getResponse().getContentAsString();
        List<String> ramItemIds = JsonPath.read(body, "$.items[?(@.productId == 'ram')].id");

        perform(delete("/orders/{id}/items/{itemId}", orderId, ramItemIds.get(0)), token)
                .andExpect(status().isNoContent());
        perform(delete("/orders/{id}/items/{itemId}", orderId, "unknown"), token)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ResourceNotFound"));

        getOrder(orderId)
                .andExpect(jsonPath("$.items[*].productId").value(contains("cpu")))
                .andExpect(jsonPath("$.totalPrice").value(299.0));
    }

    // --- O2: delivery address ---

    @Test
    void selectsASavedDeliveryAddress() throws Exception {
        String orderId = createOrder();

        selectAddress(orderId, "address-2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryAddressId").value("address-2"));

        selectAddress(orderId, "not-my-address")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ResourceNotFound"));
        perform(put("/orders/{id}/delivery-address", orderId)
                .contentType(MediaType.APPLICATION_JSON).content("{}"), token)
                .andExpect(status().isBadRequest());
        getOrder(orderId).andExpect(jsonPath("$.deliveryAddressId").value("address-2"));
    }

    @Test
    void passesOnAccountServiceErrors() throws Exception {
        String orderId = createOrder();

        given(accountClient.getSavedAddressIds(userId, token))
                .willThrow(new ServiceUnavailableException("Account down", null));
        selectAddress(orderId, "address-1")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ServiceUnavailable"));

        willThrow(new AuthenticationRequiredException("token rejected"))
                .given(accountClient).getSavedAddressIds(userId, token);
        selectAddress(orderId, "address-1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AuthenticationRequired"));
    }

    // --- O1: checkout ---

    @Test
    void checkoutNeedsItemsAndAddressThenReturnsAccepted() throws Exception {
        String orderId = createOrder();

        perform(post("/orders/{id}/checkout", orderId), token)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OrderStateViolation"));
        addItem(orderId, "cpu", 1);
        addItem(orderId, "ram", 2);
        perform(post("/orders/{id}/checkout", orderId), token)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OrderStateViolation"));
        assertThat(events.stream(OrderPlaced.class)).isEmpty();

        selectAddress(orderId, "address-1");
        perform(post("/orders/{id}/checkout", orderId), token)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"));

        assertThat(events.stream(OrderPlaced.class)).singleElement().satisfies(placed -> {
            assertThat(placed.orderId()).isEqualTo(orderId);
            assertThat(placed.userId()).isEqualTo(userId);
            assertThat(placed.items()).containsExactlyInAnyOrder(
                    new OrderPlaced.Item("cpu", 1), new OrderPlaced.Item("ram", 2));
        });
    }

    @Test
    void cartOperationsAreRejectedAfterCheckout() throws Exception {
        String orderId = placedOrder();

        addItem(orderId, "ram", 1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OrderStateViolation"));
        perform(delete("/orders/{id}/items/{itemId}", orderId, "any"), token).andExpect(status().isConflict());
        selectAddress(orderId, "address-2").andExpect(status().isConflict());
        perform(post("/orders/{id}/checkout", orderId), token).andExpect(status().isConflict());
        recordPayment(orderId, "PAID")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OrderStateViolation"));
    }

    // --- O3: reservation result ---

    @Test
    void stockReservedMovesTheOrderToProcessingWithATenMinutePaymentLimit() throws Exception {
        String orderId = placedOrder();
        Instant before = Instant.now();

        orderService.handleStockReserved(orderId);

        String body = getOrder(orderId)
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andReturn().getResponse().getContentAsString();
        Instant paymentDueAt = Instant.parse(JsonPath.read(body, "$.paymentDueAt"));
        assertThat(Duration.between(before, paymentDueAt)).isBetween(Duration.ofMinutes(10).minusSeconds(1),
                Duration.ofMinutes(10).plusSeconds(30));
    }

    @Test
    void failedReservationCancelsTheOrderWithoutOrderCancelled() throws Exception {
        String orderId = placedOrder();

        orderService.handleStockReservationFailed(orderId);

        getOrder(orderId).andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(events.stream(OrderCancelled.class)).isEmpty();
    }

    @Test
    void reservationResultsForOrdersThatAreNotPlacedAreIgnored() throws Exception {
        String orderId = placedOrder();
        perform(post("/orders/{id}/cancel", orderId), token).andExpect(status().isOk());

        orderService.handleStockReserved(orderId);
        orderService.handleStockReservationFailed(orderId);
        orderService.handleStockReserved("unknown-order");

        getOrder(orderId).andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // --- O4: payment ---

    @Test
    void successfulPaymentCompletesTheOrder() throws Exception {
        String orderId = processingOrder();

        recordPayment(orderId, "PAID")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));

        assertThat(events.stream(OrderCompleted.class)).extracting(OrderCompleted::orderId).containsExactly(orderId);
        assertThat(events.stream(PaymentStatusUpdated.class)).extracting(PaymentStatusUpdated::paymentStatus)
                .containsExactly("PAID");
        assertThat(events.stream(OrderCancelled.class)).isEmpty();
    }

    @Test
    void failedPaymentCancelsTheOrder() throws Exception {
        String orderId = processingOrder();

        recordPayment(orderId, "FAILED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.paymentStatus").value("FAILED"));

        assertThat(events.stream(OrderCancelled.class)).singleElement().satisfies(cancelled -> {
            assertThat(cancelled.orderId()).isEqualTo(orderId);
            assertThat(cancelled.reason()).isEqualTo("PAYMENT_FAILED");
        });
        assertThat(events.stream(OrderCompleted.class)).isEmpty();
    }

    @Test
    void rejectsInvalidPaymentRequests() throws Exception {
        String orderId = processingOrder();

        for (String body : List.of("{\"paymentStatus\": \"PENDING\"}", "{\"paymentStatus\": \"REFUNDED\"}", "{}")) {
            perform(put("/orders/{id}/payment", orderId).contentType(MediaType.APPLICATION_JSON).content(body), token)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("ValidationFailed"));
        }
        getOrder(orderId)
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"));

        // a payment can be recorded only once
        recordPayment(orderId, "PAID").andExpect(status().isOk());
        recordPayment(orderId, "FAILED").andExpect(status().isConflict());
    }

    @Test
    void unpaidOrderIsCancelledWhenThePaymentTimeLimitPasses() throws Exception {
        String orderId = processingOrder();

        assertThat(orderService.cancelOverduePayments(Instant.now().plus(Duration.ofMinutes(9)))).isZero();
        getOrder(orderId).andExpect(jsonPath("$.status").value("PROCESSING"));

        orderService.cancelOverduePayments(Instant.now().plus(Duration.ofMinutes(11)));

        getOrder(orderId)
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"));
        assertThat(events.stream(OrderCancelled.class)).singleElement().satisfies(cancelled -> {
            assertThat(cancelled.orderId()).isEqualTo(orderId);
            assertThat(cancelled.reason()).isEqualTo("PAYMENT_TIMEOUT");
        });
        recordPayment(orderId, "PAID").andExpect(status().isConflict());
    }

    // --- O6: cancel ---

    @Test
    void cancellingADraftPublishesNoEvent() throws Exception {
        String orderId = createOrder();

        perform(post("/orders/{id}/cancel", orderId), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(events.stream(OrderCancelled.class)).isEmpty();
    }

    @Test
    void cancellingAPlacedOrProcessingOrderPublishesOrderCancelled() throws Exception {
        String placed = placedOrder();
        perform(post("/orders/{id}/cancel", placed), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        String processing = processingOrder();
        perform(post("/orders/{id}/cancel", processing), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(events.stream(OrderCancelled.class)).extracting(OrderCancelled::orderId)
                .containsExactly(placed, processing);
        assertThat(events.stream(OrderCancelled.class)).extracting(OrderCancelled::reason)
                .containsOnly("CUSTOMER_CANCELLED");
    }

    @Test
    void fulfilledOrCancelledOrdersCannotBeCancelled() throws Exception {
        String completed = processingOrder();
        recordPayment(completed, "PAID");

        perform(post("/orders/{id}/cancel", completed), token)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OrderStateViolation"));
        getOrder(completed).andExpect(jsonPath("$.status").value("COMPLETED"));

        String cancelled = createOrder();
        perform(post("/orders/{id}/cancel", cancelled), token).andExpect(status().isOk());
        perform(post("/orders/{id}/cancel", cancelled), token).andExpect(status().isConflict());
    }

    // --- O5: tracking and history ---

    @Test
    void listsTheCustomersOrders() throws Exception {
        String first = processingOrder();
        String second = createOrder();

        perform(get("/users/{userId}/orders", userId), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(containsInAnyOrder(first, second)))
                .andExpect(jsonPath("$[?(@.id == '" + first + "')].status").value(contains("PROCESSING")))
                .andExpect(jsonPath("$[?(@.id == '" + second + "')].status").value(contains("DRAFT")));
    }

    // --- R3: cart from a recommended build ---

    @Test
    void createsACartFromARecommendedBuild() throws Exception {
        given(recommendationClient.getRecommendedBuild("rec-1", token))
                .willReturn(new RecommendedBuild("rec-1", userId, List.of("cpu", "ram")));

        perform(post("/orders/from-recommendation/{id}", "rec-1"), token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.items[*].productId").value(containsInAnyOrder("cpu", "ram")))
                .andExpect(jsonPath("$.items[*].quantity").value(contains(1, 1)))
                .andExpect(jsonPath("$.totalPrice").value(379.0));
    }

    @Test
    void addsARecommendedBuildToTheExistingCart() throws Exception {
        String orderId = createOrder();
        addItem(orderId, "ram", 1);
        given(recommendationClient.getRecommendedBuild("rec-1", token))
                .willReturn(new RecommendedBuild("rec-1", userId, List.of("cpu", "ram")));

        perform(post("/orders/from-recommendation/{id}", "rec-1"), token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[?(@.productId == 'ram')].quantity").value(contains(2)))
                .andExpect(jsonPath("$.totalPrice").value(459.0));
    }

    @Test
    void passesOnRecommendationServiceErrors() throws Exception {
        given(recommendationClient.getRecommendedBuild("unknown", token))
                .willThrow(new ResourceNotFoundException("Recommendation not found: unknown"));
        given(recommendationClient.getRecommendedBuild("not-mine", token))
                .willThrow(new AccessDeniedException("belongs to another user"));
        given(recommendationClient.getRecommendedBuild("down", token))
                .willThrow(new ServiceUnavailableException("Recommendation down", null));
        given(recommendationClient.getRecommendedBuild("gone-product", token))
                .willReturn(new RecommendedBuild("gone-product", userId, List.of("cpu", "removed-from-catalog")));

        perform(post("/orders/from-recommendation/{id}", "unknown"), token)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("ResourceNotFound"));
        perform(post("/orders/from-recommendation/{id}", "not-mine"), token)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("AccessDenied"));
        perform(post("/orders/from-recommendation/{id}", "down"), token)
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("ServiceUnavailable"));
        perform(post("/orders/from-recommendation/{id}", "gone-product"), token)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("ResourceNotFound"));

        // nothing was created
        perform(get("/users/{userId}/orders", userId), token).andExpect(jsonPath("$.length()").value(0));
    }

    // --- access control ---

    @Test
    void everyEndpointNeedsAToken() throws Exception {
        String orderId = createOrder();

        List<MockHttpServletRequestBuilder> requests = List.of(
                post("/orders").contentType(MediaType.APPLICATION_JSON).content("{\"userId\": \"" + userId + "\"}"),
                post("/orders/{id}/items", orderId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": \"cpu\", \"quantity\": 1}"),
                delete("/orders/{id}/items/{itemId}", orderId, "item"),
                post("/orders/{id}/checkout", orderId),
                put("/orders/{id}/delivery-address", orderId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\": \"address-1\"}"),
                put("/orders/{id}/payment", orderId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentStatus\": \"PAID\"}"),
                get("/orders/{id}", orderId),
                get("/users/{userId}/orders", userId),
                post("/orders/{id}/cancel", orderId),
                post("/orders/from-recommendation/{id}", "rec-1"));
        for (MockHttpServletRequestBuilder request : requests) {
            mockMvc.perform(request)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AuthenticationRequired"));
        }
        mockMvc.perform(get("/orders/{id}", orderId).header(HttpHeaders.AUTHORIZATION, "Bearer not-a-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AuthenticationRequired"));
    }

    @Test
    void anotherUsersOrdersAreForbiddenAndUnknownOrdersAreNotFound() throws Exception {
        String orderId = createOrder();

        perform(get("/orders/{id}", orderId), otherUserToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AccessDenied"));
        perform(post("/orders/{id}/items", orderId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\": \"cpu\", \"quantity\": 1}"), otherUserToken)
                .andExpect(status().isForbidden());
        perform(post("/orders/{id}/cancel", orderId), otherUserToken).andExpect(status().isForbidden());
        perform(post("/orders/{id}/checkout", orderId), otherUserToken).andExpect(status().isForbidden());
        // owner given by the body or the path: checked before anything is looked up
        perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\": \"" + userId + "\"}"), otherUserToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AccessDenied"));
        perform(get("/users/{userId}/orders", userId), otherUserToken).andExpect(status().isForbidden());

        perform(get("/orders/{id}", "unknown"), token)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ResourceNotFound"));
        getOrder(orderId)
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.items.length()").value(0));
        verify(catalogClient, never()).findProduct(any());
    }

    // --- helpers ---

    private ResultActions perform(MockHttpServletRequestBuilder request, String bearer) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private ResultActions createOrderRequest() throws Exception {
        return perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\": \"" + userId + "\"}"), token);
    }

    private String createOrder() throws Exception {
        return JsonPath.read(createOrderRequest().andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    private ResultActions addItem(String orderId, String productId, int quantity) throws Exception {
        return perform(post("/orders/{id}/items", orderId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\": \"" + productId + "\", \"quantity\": " + quantity + "}"), token);
    }

    private ResultActions selectAddress(String orderId, String addressId) throws Exception {
        return perform(put("/orders/{id}/delivery-address", orderId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"addressId\": \"" + addressId + "\"}"), token);
    }

    private ResultActions recordPayment(String orderId, String paymentStatus) throws Exception {
        return perform(put("/orders/{id}/payment", orderId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"paymentStatus\": \"" + paymentStatus + "\"}"), token);
    }

    private ResultActions getOrder(String orderId) throws Exception {
        return perform(get("/orders/{id}", orderId), token).andExpect(status().isOk());
    }

    private String placedOrder() throws Exception {
        String orderId = createOrder();
        addItem(orderId, "cpu", 1).andExpect(status().isCreated());
        selectAddress(orderId, "address-1").andExpect(status().isOk());
        perform(post("/orders/{id}/checkout", orderId), token).andExpect(status().isAccepted());
        return orderId;
    }

    private String processingOrder() throws Exception {
        String orderId = placedOrder();
        orderService.handleStockReserved(orderId);
        return orderId;
    }
}
