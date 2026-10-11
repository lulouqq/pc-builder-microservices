package com.csci318.catalogservice;

import com.csci318.catalogservice.domain.Product;
import com.csci318.catalogservice.domain.Specification;
import com.csci318.catalogservice.domain.event.ProductCreated;
import com.csci318.catalogservice.domain.event.ProductRemoved;
import com.csci318.catalogservice.domain.event.ProductUpdated;
import com.csci318.catalogservice.infrastructure.CategoryRepository;
import com.csci318.catalogservice.infrastructure.ProductRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * C5: manage products and categories.
 */
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
class CatalogManagementTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ApplicationEvents events;

    @BeforeEach
    void clearDatabase() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void createsUpdatesAndRemovesCategory() throws Exception {
        String categoryId = createCategory("CPU");

        mockMvc.perform(put("/categories/{id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Processors\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoryId))
                .andExpect(jsonPath("$.name").value("Processors"));

        mockMvc.perform(delete("/categories/{id}", categoryId))
                .andExpect(status().isNoContent());
        assertThat(categoryRepository.existsById(categoryId)).isFalse();
    }

    @Test
    void createsProductWithSpecifications() throws Exception {
        String categoryId = createCategory("CPU");

        String productId = createProduct(categoryId);

        Product product = productRepository.findById(productId).orElseThrow();
        assertThat(product.getName()).isEqualTo("Ryzen 5 7600");
        assertThat(product.getCategoryId()).isEqualTo(categoryId);
        assertThat(product.getSpecifications()).containsExactly(new Specification("Socket", "AM5"));
        assertThat(events.stream(ProductCreated.class).map(ProductCreated::productId)).containsExactly(productId);
    }

    @Test
    void updatesProductAndReplacesSpecifications() throws Exception {
        String categoryId = createCategory("CPU");
        String productId = createProduct(categoryId);

        mockMvc.perform(put("/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Ryzen 5 7600X", 329.0, categoryId, "Cores", "6")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value("Ryzen 5 7600X"))
                .andExpect(jsonPath("$.price").value(329.0))
                .andExpect(jsonPath("$.specifications.length()").value(1))
                .andExpect(jsonPath("$.specifications[0].name").value("Cores"));

        assertThat(productRepository.findById(productId).orElseThrow().getSpecifications())
                .containsExactly(new Specification("Cores", "6"));
        assertThat(events.stream(ProductUpdated.class).map(ProductUpdated::productId)).containsExactly(productId);
    }

    @Test
    void removesProduct() throws Exception {
        String productId = createProduct(createCategory("CPU"));

        mockMvc.perform(delete("/products/{id}", productId))
                .andExpect(status().isNoContent());

        assertThat(productRepository.existsById(productId)).isFalse();
        assertThat(events.stream(ProductRemoved.class).map(ProductRemoved::productId)).containsExactly(productId);
    }

    @Test
    void rejectsInvalidProduct() throws Exception {
        String categoryId = createCategory("CPU");

        // missing name and no specifications
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\": 10, \"brand\": \"AMD\", \"categoryId\": \"" + categoryId
                                + "\", \"specifications\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ValidationFailed"));

        // category does not exist
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Ryzen 5 7600", 299.0, "no-such-category", "Socket", "AM5")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ValidationFailed"));

        assertThat(productRepository.count()).isZero();
    }

    @Test
    void rejectsInvalidCategory() throws Exception {
        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ValidationFailed"));
    }

    @Test
    void returnsNotFoundForUnknownProductOrCategory() throws Exception {
        String categoryId = createCategory("CPU");

        mockMvc.perform(put("/products/{id}", "unknown")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Ryzen 5 7600", 299.0, categoryId, "Socket", "AM5")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ResourceNotFound"));
        mockMvc.perform(delete("/products/{id}", "unknown"))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/categories/{id}", "unknown")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"GPU\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/categories/{id}", "unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ResourceNotFound"));
    }

    @Test
    void doesNotRemoveCategoryThatStillHasProducts() throws Exception {
        String categoryId = createCategory("CPU");
        createProduct(categoryId);

        mockMvc.perform(delete("/categories/{id}", categoryId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CategoryInUse"));

        assertThat(categoryRepository.existsById(categoryId)).isTrue();
    }

    @Test
    void usesContractErrorCodesForFrameworkErrors() throws Exception {
        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ValidationFailed"));
        mockMvc.perform(delete("/no-such-path"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ResourceNotFound"));
    }

    private String createCategory(String name) throws Exception {
        String body = mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String createProduct(String categoryId) throws Exception {
        String body = mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Ryzen 5 7600", 299.0, categoryId, "Socket", "AM5")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Ryzen 5 7600"))
                .andExpect(jsonPath("$.price").value(299.0))
                .andExpect(jsonPath("$.brand").value("AMD"))
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.specifications[0].name").value("Socket"))
                .andExpect(jsonPath("$.specifications[0].value").value("AM5"))
                .andExpect(jsonPath("$.stockStatus").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String productJson(String name, double price, String categoryId, String specName, String specValue) {
        return """
                {"name": "%s", "price": %s, "brand": "AMD", "categoryId": "%s",
                 "specifications": [{"name": "%s", "value": "%s"}]}
                """.formatted(name, price, categoryId, specName, specValue);
    }
}
