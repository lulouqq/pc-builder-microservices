package com.csci318.catalogservice;

import com.csci318.catalogservice.domain.event.ProductViewed;
import com.csci318.catalogservice.infrastructure.ProductRepository;
import com.csci318.catalogservice.infrastructure.ProductViewRepository;
import com.csci318.catalogservice.infrastructure.ProductViewedKafkaListener;
import com.csci318.catalogservice.infrastructure.ProductViewedKafkaPublisher;
import com.csci318.catalogservice.service.TrendingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.support.SendResult;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * The ProductViewed message on catalog-events (Architecture Decision 16), without a broker: the
 * publisher's message and failure handling, and the listener's parsing.
 */
class ProductViewedKafkaMessagingTests {

    private static final Instant OCCURRED_AT = Instant.parse("2026-10-14T03:15:00Z");

    @SuppressWarnings("unchecked")
    private final KafkaOperations<String, String> kafka = mock(KafkaOperations.class);
    private final List<RecordedView> recorded = new ArrayList<>();
    /** Records the calls instead of storing views (a concrete class, so it is not mocked). */
    private final TrendingService trendingService = new TrendingService(mock(ProductViewRepository.class),
            mock(ProductRepository.class), Duration.ofHours(24), 10) {
        @Override
        public void recordView(String eventId, ProductViewed view) {
            recorded.add(new RecordedView(eventId, view));
        }
    };
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProductViewedKafkaPublisher publisher = new ProductViewedKafkaPublisher(kafka, objectMapper);
    private final ProductViewedKafkaListener listener = new ProductViewedKafkaListener(trendingService, objectMapper);

    @Test
    void publishesProductViewedKeyedByProductId() {
        given(kafka.send(anyString(), anyString(), anyString()))
                .willReturn(new CompletableFuture<SendResult<String, String>>());

        publisher.on(new ProductViewed("product-1", OCCURRED_AT));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(kafka).send(eq("catalog-events"), eq("product-1"), message.capture());
        String json = message.getValue();
        assertThat((String) JsonPath.read(json, "$.eventId")).isNotBlank();
        assertThat((String) JsonPath.read(json, "$.eventType")).isEqualTo("ProductViewed");
        assertThat((Integer) JsonPath.read(json, "$.version")).isEqualTo(1);
        assertThat((String) JsonPath.read(json, "$.occurredAt")).isEqualTo("2026-10-14T03:15:00Z");
        assertThat((String) JsonPath.read(json, "$.productId")).isEqualTo("product-1");
    }

    @Test
    void aSendThatThrowsIsNotRethrown() {
        given(kafka.send(anyString(), anyString(), anyString())).willThrow(new IllegalStateException("broker down"));

        assertThatNoException().isThrownBy(() -> publisher.on(new ProductViewed("product-1", OCCURRED_AT)));
    }

    @Test
    void aSendThatFailsLaterIsNotRethrown() {
        given(kafka.send(anyString(), anyString(), anyString()))
                .willReturn(CompletableFuture.failedFuture(new IllegalStateException("timeout")));
        assertThatNoException().isThrownBy(() -> publisher.on(new ProductViewed("product-1", OCCURRED_AT)));
    }

    @Test
    void listenerRecordsTheView() {
        listener.onMessage("""
                {"eventId": "event-1", "eventType": "ProductViewed", "version": 1,
                 "occurredAt": "2026-10-14T03:15:00Z", "productId": "product-1"}
                """);

        assertThat(recorded).containsExactly(
                new RecordedView("event-1", new ProductViewed("product-1", OCCURRED_AT)));
    }

    @Test
    void listenerSkipsMessagesItCannotUse() {
        for (String message : List.of(
                "not json",
                "{\"eventId\": \"e\", \"eventType\": \"SomethingElse\", \"occurredAt\": \"2026-10-14T03:15:00Z\","
                        + " \"productId\": \"p\"}",
                "{\"eventType\": \"ProductViewed\", \"occurredAt\": \"2026-10-14T03:15:00Z\", \"productId\": \"p\"}",
                "{\"eventId\": \"e\", \"eventType\": \"ProductViewed\", \"occurredAt\": \"2026-10-14T03:15:00Z\"}",
                "{\"eventId\": \"e\", \"eventType\": \"ProductViewed\", \"occurredAt\": \"yesterday\","
                        + " \"productId\": \"p\"}")) {
            listener.onMessage(message);
        }

        assertThat(recorded).isEmpty();
    }

    private record RecordedView(String eventId, ProductViewed view) {
    }
}
