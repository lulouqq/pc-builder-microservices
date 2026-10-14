package com.csci318.catalogservice;

import com.csci318.catalogservice.domain.Category;
import com.csci318.catalogservice.domain.Product;
import com.csci318.catalogservice.domain.Specification;
import com.csci318.catalogservice.domain.StockStatus;
import com.csci318.catalogservice.infrastructure.CategoryRepository;
import com.csci318.catalogservice.infrastructure.ProductRepository;
import com.csci318.catalogservice.infrastructure.ProductViewRepository;
import com.csci318.catalogservice.service.InventoryClient;
import com.jayway.jsonpath.JsonPath;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * C3 through a real (embedded) Kafka broker: viewing product details publishes ProductViewed to
 * catalog-events, and Catalog Service's own listener records it for the trending list.
 */
@SpringBootTest(properties = {
        "messaging.kafka.enabled=true",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = "catalog-events")
@DirtiesContext
class CatalogKafkaIntegrationTests {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductViewRepository viewRepository;

    @MockBean
    private InventoryClient inventoryClient;

    private Consumer<String, String> catalogEvents;
    private String productId;

    @BeforeEach
    void setUp() {
        catalogEvents = new DefaultKafkaConsumerFactory<>(
                KafkaTestUtils.consumerProps("test-" + UUID.randomUUID(), "true", broker),
                new StringDeserializer(), new StringDeserializer()).createConsumer();
        broker.consumeFromAnEmbeddedTopic(catalogEvents, "catalog-events");

        given(inventoryClient.getStockStatus(any())).willReturn(StockStatus.IN_STOCK);
        String categoryId = categoryRepository.save(new Category("CPU")).getId();
        productId = productRepository.save(new Product("Ryzen 5 7600", 299f, "AMD", categoryId,
                List.of(new Specification("Socket", "AM5")))).getId();
    }

    @AfterEach
    void closeConsumer() {
        catalogEvents.close();
    }

    @Test
    void viewingProductDetailsGoesThroughKafkaToTheTrendingList() throws Exception {
        mockMvc.perform(get("/products/{id}", productId))
                .andExpect(status().isOk());

        ConsumerRecord<String, String> viewed = nextFor(productId);
        assertThat((String) JsonPath.read(viewed.value(), "$.eventType")).isEqualTo("ProductViewed");
        assertThat((String) JsonPath.read(viewed.value(), "$.eventId")).isNotBlank();
        assertThat((Integer) JsonPath.read(viewed.value(), "$.version")).isEqualTo(1);
        assertThat((String) JsonPath.read(viewed.value(), "$.occurredAt")).isNotBlank();

        await().atMost(TIMEOUT).untilAsserted(() ->
                mockMvc.perform(get("/products/trending"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].id").value(productId)));
    }

    @Test
    void aRepeatedMessageIsRecordedOnce() {
        long viewsBefore = viewRepository.count();
        String eventId = UUID.randomUUID().toString();
        String message = "{\"eventId\": \"" + eventId + "\", \"eventType\": \"ProductViewed\", \"version\": 1, "
                + "\"occurredAt\": \"" + Instant.now() + "\", \"productId\": \"" + productId + "\"}";

        kafkaTemplate.send("catalog-events", productId, message);
        kafkaTemplate.send("catalog-events", productId, message);
        String marker = UUID.randomUUID().toString();
        kafkaTemplate.send("catalog-events", productId, message.replace(eventId, marker));

        // the third message is consumed after the first two (same key, same partition)
        await().atMost(TIMEOUT).until(() -> viewRepository.existsById(marker));
        assertThat(viewRepository.existsById(eventId)).isTrue();
        assertThat(viewRepository.count()).isEqualTo(viewsBefore + 2);
    }

    /** The next ProductViewed message for the given product. */
    private ConsumerRecord<String, String> nextFor(String key) {
        long deadline = System.currentTimeMillis() + TIMEOUT.toMillis();
        while (System.currentTimeMillis() < deadline) {
            for (ConsumerRecord<String, String> record : catalogEvents.poll(Duration.ofMillis(200))) {
                if (key.equals(record.key())) {
                    assertThat((String) JsonPath.read(record.value(), "$.productId")).isEqualTo(key);
                    return record;
                }
            }
        }
        throw new AssertionError("No ProductViewed received for product " + key + " within " + TIMEOUT);
    }
}
