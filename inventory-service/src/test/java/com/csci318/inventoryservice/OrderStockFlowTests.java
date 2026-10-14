package com.csci318.inventoryservice;

import com.csci318.inventoryservice.domain.ReservationStatus;
import com.csci318.inventoryservice.domain.StockReservation;
import com.csci318.inventoryservice.domain.event.OrderStockReservationFailed;
import com.csci318.inventoryservice.domain.event.OrderStockReservationFailed.UnavailableItem;
import com.csci318.inventoryservice.domain.event.OrderStockReserved;
import com.csci318.inventoryservice.infrastructure.InventoryReadModel;
import com.csci318.inventoryservice.infrastructure.OrderEventKafkaListener;
import com.csci318.inventoryservice.infrastructure.StockReservationRepository;
import com.csci318.inventoryservice.service.GetInventoryByProductQuery;
import com.csci318.inventoryservice.service.InventoryCommandService;
import com.csci318.inventoryservice.service.InventoryQueryService;
import com.csci318.inventoryservice.service.UpdateStockLevelCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The inventory side of the order flow, driven by the JSON messages of the order-events topic
 * (Architecture Decision 14). No broker is used: the messages are handed to the listener directly.
 */
@SpringBootTest
@ActiveProfiles("test")
@RecordApplicationEvents
class OrderStockFlowTests {

    @Autowired
    private OrderEventKafkaListener listener;

    @Autowired
    private InventoryCommandService commandService;

    @Autowired
    private InventoryQueryService queryService;

    @Autowired
    private StockReservationRepository reservations;

    @Autowired
    private ApplicationEvents events;

    private String orderId;
    private String cpu;
    private String ram;

    @BeforeEach
    void stockProducts() {
        orderId = "order-" + UUID.randomUUID();
        cpu = "cpu-" + UUID.randomUUID();
        ram = "ram-" + UUID.randomUUID();
        commandService.handle(new UpdateStockLevelCommand(cpu, 20, null));
        commandService.handle(new UpdateStockLevelCommand(ram, 20, null));
    }

    @Test
    void orderPlacedReservesStockForEveryItem() {
        listener.onMessage(orderPlaced(orderId, item(cpu, 1), item(ram, 2)));

        assertThat(stock(cpu).getReservedQuantity()).isEqualTo(1);
        assertThat(stock(ram).getReservedQuantity()).isEqualTo(2);
        List<StockReservation> active = reservations.findByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE);
        assertThat(active).extracting(StockReservation::getProductId).containsExactlyInAnyOrder(cpu, ram);
        assertThat(events.stream(OrderStockReserved.class)).singleElement().satisfies(reserved -> {
            assertThat(reserved.orderId()).isEqualTo(orderId);
            assertThat(reserved.reservations()).extracting(OrderStockReserved.Reservation::productId)
                    .containsExactly(cpu, ram);
            assertThat(reserved.reservations()).extracting(OrderStockReserved.Reservation::quantity)
                    .containsExactly(1, 2);
            assertThat(reserved.reservations()).extracting(OrderStockReserved.Reservation::reservationId)
                    .containsExactlyInAnyOrderElementsOf(active.stream().map(StockReservation::getId).toList());
        });
        assertThat(events.stream(OrderStockReservationFailed.class)).isEmpty();
    }

    @Test
    void nothingIsReservedWhenOneItemIsUnavailable() {
        String unknownProduct = "unknown-" + UUID.randomUUID();

        listener.onMessage(orderPlaced(orderId, item(cpu, 1), item(ram, 21), item(unknownProduct, 1)));

        assertThat(stock(cpu).getReservedQuantity()).isZero();
        assertThat(stock(ram).getReservedQuantity()).isZero();
        assertThat(reservations.existsByOrderId(orderId)).isFalse();
        assertThat(events.stream(OrderStockReservationFailed.class)).singleElement().satisfies(failed -> {
            assertThat(failed.orderId()).isEqualTo(orderId);
            assertThat(failed.reason()).isEqualTo("STOCK_UNAVAILABLE");
            assertThat(failed.unavailableItems()).containsExactly(
                    new UnavailableItem(ram, 21, 20), new UnavailableItem(unknownProduct, 1, 0));
        });
        assertThat(events.stream(OrderStockReserved.class)).isEmpty();
    }

    @Test
    void stockAlreadyReservedByOtherOrdersIsNotAvailable() {
        listener.onMessage(orderPlaced("first-" + orderId, item(cpu, 15)));

        listener.onMessage(orderPlaced(orderId, item(cpu, 6)));

        assertThat(stock(cpu).getReservedQuantity()).isEqualTo(15);
        assertThat(events.stream(OrderStockReservationFailed.class)).singleElement()
                .satisfies(failed -> assertThat(failed.unavailableItems())
                        .containsExactly(new UnavailableItem(cpu, 6, 5)));
    }

    @Test
    void repeatedOrderPlacedDoesNotReserveTwice() {
        listener.onMessage(orderPlaced(orderId, item(cpu, 3)));
        listener.onMessage(orderPlaced(orderId, item(cpu, 3)));

        assertThat(stock(cpu).getReservedQuantity()).isEqualTo(3);
        assertThat(events.stream(OrderStockReserved.class)).hasSize(1);
    }

    @Test
    void orderCancelledReleasesTheOrdersReservations() {
        listener.onMessage(orderPlaced(orderId, item(cpu, 1), item(ram, 2)));
        listener.onMessage(orderPlaced("other-" + orderId, item(cpu, 4)));

        listener.onMessage(orderEvent("OrderCancelled", orderId, "\"reason\": \"CUSTOMER_CANCELLED\""));

        assertThat(stock(cpu).getQuantity()).isEqualTo(20);
        assertThat(stock(cpu).getReservedQuantity()).isEqualTo(4);
        assertThat(stock(ram).getReservedQuantity()).isZero();
        assertThat(reservations.findByOrderIdAndStatus(orderId, ReservationStatus.RELEASED)).hasSize(2);
        assertThat(reservations.findByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE)).isEmpty();
    }

    @Test
    void orderCompletedConfirmsTheReservationsAndDeductsTheStock() {
        listener.onMessage(orderPlaced(orderId, item(cpu, 1), item(ram, 2)));

        listener.onMessage(orderEvent("OrderCompleted", orderId, null));

        assertThat(stock(cpu).getQuantity()).isEqualTo(19);
        assertThat(stock(cpu).getReservedQuantity()).isZero();
        assertThat(stock(ram).getQuantity()).isEqualTo(18);
        assertThat(stock(ram).getReservedQuantity()).isZero();
        assertThat(reservations.findByOrderIdAndStatus(orderId, ReservationStatus.CONFIRMED)).hasSize(2);
    }

    @Test
    void cancelOrCompleteWithoutActiveReservationsChangesNothing() {
        listener.onMessage(orderEvent("OrderCancelled", orderId, "\"reason\": \"PAYMENT_TIMEOUT\""));
        listener.onMessage(orderEvent("OrderCompleted", orderId, null));

        // released reservations are not confirmed afterwards
        listener.onMessage(orderPlaced(orderId, item(cpu, 2)));
        listener.onMessage(orderEvent("OrderCancelled", orderId, "\"reason\": \"PAYMENT_FAILED\""));
        listener.onMessage(orderEvent("OrderCompleted", orderId, null));
        listener.onMessage(orderEvent("OrderCancelled", orderId, "\"reason\": \"PAYMENT_FAILED\""));

        assertThat(stock(cpu).getQuantity()).isEqualTo(20);
        assertThat(stock(cpu).getReservedQuantity()).isZero();
    }

    @Test
    void unusableMessagesAreSkipped() {
        listener.onMessage("not json");
        listener.onMessage("{\"eventType\": \"OrderPlaced\", \"items\": [{\"productId\": \"" + cpu
                + "\", \"quantity\": 1}]}");
        listener.onMessage(orderEvent("SomethingElse", orderId, null));

        assertThat(stock(cpu).getReservedQuantity()).isZero();
        assertThat(events.stream(OrderStockReserved.class)).isEmpty();

        // an order without usable items fails instead of being reserved
        listener.onMessage(orderPlaced(orderId));
        listener.onMessage(orderPlaced("zero-" + orderId, item(cpu, 0)));
        assertThat(events.stream(OrderStockReservationFailed.class)).hasSize(2);
        assertThat(stock(cpu).getReservedQuantity()).isZero();
    }

    private InventoryReadModel stock(String productId) {
        return queryService.handle(new GetInventoryByProductQuery(productId));
    }

    private static String item(String productId, int quantity) {
        return "{\"productId\": \"" + productId + "\", \"quantity\": " + quantity + "}";
    }

    private static String orderPlaced(String orderId, String... items) {
        return orderEvent("OrderPlaced", orderId,
                "\"userId\": \"user-1\", \"items\": [" + String.join(", ", items) + "]");
    }

    /** A message as Order Service publishes it: the common fields plus the event's own fields. */
    private static String orderEvent(String eventType, String orderId, String extraFields) {
        return "{\"eventId\": \"" + UUID.randomUUID() + "\", \"eventType\": \"" + eventType
                + "\", \"version\": 1, \"occurredAt\": \"2026-10-14T03:15:00Z\", \"orderId\": \"" + orderId + "\""
                + (extraFields == null ? "" : ", " + extraFields) + "}";
    }
}
