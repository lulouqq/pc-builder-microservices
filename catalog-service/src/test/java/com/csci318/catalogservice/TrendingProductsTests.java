package com.csci318.catalogservice;

import com.csci318.catalogservice.domain.Category;
import com.csci318.catalogservice.domain.Product;
import com.csci318.catalogservice.domain.Specification;
import com.csci318.catalogservice.domain.StockStatus;
import com.csci318.catalogservice.domain.event.ProductViewed;
import com.csci318.catalogservice.infrastructure.CategoryRepository;
import com.csci318.catalogservice.infrastructure.ProductRepository;
import com.csci318.catalogservice.infrastructure.ProductViewRepository;
import com.csci318.catalogservice.service.InventoryClient;
import com.csci318.catalogservice.service.TrendingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * C3: trending products, with Kafka messaging disabled, so views are recorded in-process
 * (Architecture Decision 16). The Kafka path is tested in CatalogKafkaIntegrationTests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RecordApplicationEvents
class TrendingProductsTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductViewRepository viewRepository;

    @Autowired
    private TrendingService trendingService;

    @Autowired
    private ApplicationEvents events;

    @MockBean
    private InventoryClient inventoryClient;

    private String categoryId;
    private String cpu;
    private String gpu;
    private String ram;

    @BeforeEach
    void seedCatalog() {
        viewRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        given(inventoryClient.getStockStatus(any())).willReturn(StockStatus.IN_STOCK);

        categoryId = categoryRepository.save(new Category("Parts")).getId();
        cpu = createProduct("Ryzen 5 7600");
        gpu = createProduct("Radeon RX 7800 XT");
        ram = createProduct("32GB DDR5");
    }

    @Test
    void viewingProductDetailsMakesTheProductTrend() throws Exception {
        view(gpu, 3);
        view(cpu, 1);
        view(ram, 2);

        // public: no token
        mockMvc.perform(get("/products/trending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(gpu))
                .andExpect(jsonPath("$[1].id").value(ram))
                .andExpect(jsonPath("$[2].id").value(cpu));
        assertThat(events.stream(ProductViewed.class)).hasSize(6);
    }

    @Test
    void trendingListUsesTheUnchangedProductResponse() throws Exception {
        view(cpu, 1);

        mockMvc.perform(get("/products/trending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(cpu))
                .andExpect(jsonPath("$[0].name").value("Ryzen 5 7600"))
                .andExpect(jsonPath("$[0].price").value(100.0))
                .andExpect(jsonPath("$[0].brand").value("Brand"))
                .andExpect(jsonPath("$[0].categoryId").value(categoryId))
                .andExpect(jsonPath("$[0].specifications[0].name").value("Socket"))
                .andExpect(jsonPath("$[0].stockStatus").doesNotExist());
    }

    @Test
    void returnsEmptyListWhenNothingWasViewed() throws Exception {
        mockMvc.perform(get("/products/trending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void unknownProductIsNotCountedAsAView() throws Exception {
        mockMvc.perform(get("/products/{id}", "unknown"))
                .andExpect(status().isNotFound());

        assertThat(events.stream(ProductViewed.class)).isEmpty();
        assertThat(viewRepository.count()).isZero();
    }

    @Test
    void productListsAndTrendingRequestsAreNotViews() throws Exception {
        mockMvc.perform(get("/products")).andExpect(status().isOk());
        mockMvc.perform(get("/products/trending")).andExpect(status().isOk());

        assertThat(events.stream(ProductViewed.class)).isEmpty();
        assertThat(viewRepository.count()).isZero();
    }

    @Test
    void everyViewCounts() throws Exception {
        view(cpu, 5);

        assertThat(viewRepository.count()).isEqualTo(5);
    }

    @Test
    void removedProductsAreLeftOutAndTheListCanBeShorter() throws Exception {
        view(gpu, 3);
        view(cpu, 2);
        view(ram, 1);

        productRepository.deleteById(gpu);

        mockMvc.perform(get("/products/trending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(cpu))
                .andExpect(jsonPath("$[1].id").value(ram));
    }

    @Test
    void returnsAtMostTheConfiguredSizeAndSkipsRemovedProductsBeforeLimiting() throws Exception {
        // 12 viewed products, the default size is 10
        List<String> products = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            String productId = createProduct("Product " + i);
            products.add(productId);
            record(productId, Instant.now().minusSeconds(60), 12 - i);
        }

        mockMvc.perform(get("/products/trending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].id").value(products.get(0)))
                .andExpect(jsonPath("$[9].id").value(products.get(9)));

        // the most viewed product is removed: the 11th moves up instead of the list getting shorter
        productRepository.deleteById(products.get(0));
        mockMvc.perform(get("/products/trending"))
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].id").value(products.get(1)))
                .andExpect(jsonPath("$[9].id").value(products.get(10)));
    }

    @Test
    void onlyViewsInsideThe24HourWindowCount() throws Exception {
        record(cpu, Instant.now().minus(Duration.ofHours(25)), 5);
        record(gpu, Instant.now().minus(Duration.ofHours(23)), 1);

        mockMvc.perform(get("/products/trending"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(gpu));
    }

    @Test
    void aRepeatedEventIsStoredOnce() {
        ProductViewed view = new ProductViewed(cpu, Instant.now());

        trendingService.recordView("event-1", view);
        trendingService.recordView("event-1", view);

        assertThat(viewRepository.count()).isEqualTo(1);
    }

    private void view(String productId, int times) throws Exception {
        for (int i = 0; i < times; i++) {
            mockMvc.perform(get("/products/{id}", productId)).andExpect(status().isOk());
        }
    }

    private void record(String productId, Instant occurredAt, int times) {
        for (int i = 0; i < times; i++) {
            trendingService.recordView(UUID.randomUUID().toString(), new ProductViewed(productId, occurredAt));
        }
    }

    private String createProduct(String name) {
        return productRepository.save(new Product(name, 100f, "Brand", categoryId,
                List.of(new Specification("Socket", "AM5")))).getId();
    }
}
