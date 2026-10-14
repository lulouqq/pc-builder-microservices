package com.csci318.catalogservice;

import com.csci318.catalogservice.domain.Category;
import com.csci318.catalogservice.domain.Product;
import com.csci318.catalogservice.domain.Specification;
import com.csci318.catalogservice.domain.event.ProductReviewed;
import com.csci318.catalogservice.infrastructure.CategoryRepository;
import com.csci318.catalogservice.infrastructure.ProductRepository;
import com.csci318.catalogservice.infrastructure.ReviewRepository;
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

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * C4: submit and view product reviews.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RecordApplicationEvents
class ProductReviewTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ApplicationEvents events;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    private String productId;
    private String otherProductId;
    private String user1;
    private String user2;
    private String user3;
    private String admin;

    @BeforeEach
    void seedCatalog() {
        reviewRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        String categoryId = categoryRepository.save(new Category("CPU")).getId();
        productId = productRepository.save(new Product("Ryzen 5 7600", 299f, "AMD", categoryId,
                List.of(new Specification("Socket", "AM5")))).getId();
        otherProductId = productRepository.save(new Product("Core i5-14600K", 449f, "Intel", categoryId,
                List.of(new Specification("Socket", "LGA1700")))).getId();

        user1 = TestTokens.bearer(jwtSecret, "user-1", "CUSTOMER");
        user2 = TestTokens.bearer(jwtSecret, "user-2", "CUSTOMER");
        user3 = TestTokens.bearer(jwtSecret, "user-3", "CUSTOMER");
        admin = TestTokens.bearer(jwtSecret, "admin-1", "ADMIN");
    }

    @Test
    void submitsReview() throws Exception {
        submitReview(user1, productId, "{\"userId\": \"user-1\", \"rating\": 5, \"comment\": \"Great CPU\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Great CPU"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        assertThat(reviewRepository.count()).isEqualTo(1);
        assertThat(events.stream(ProductReviewed.class))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.productId()).isEqualTo(productId);
                    assertThat(event.userId()).isEqualTo("user-1");
                    assertThat(event.rating()).isEqualTo(5);
                });
    }

    @Test
    void commentIsOptional() throws Exception {
        submitReview(user1, productId, "{\"userId\": \"user-1\", \"rating\": 1}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(1))
                .andExpect(jsonPath("$.comment").doesNotExist());
    }

    @Test
    void listsOnlyTheReviewsOfTheProduct() throws Exception {
        submitReview(user1, productId, "{\"userId\": \"user-1\", \"rating\": 5, \"comment\": \"Great CPU\"}");
        submitReview(user2, productId, "{\"userId\": \"user-2\", \"rating\": 3}");
        submitReview(user3, otherProductId, "{\"userId\": \"user-3\", \"rating\": 4}");

        mockMvc.perform(get("/products/{id}/reviews", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].userId").value(containsInAnyOrder("user-1", "user-2")));
        mockMvc.perform(get("/products/{id}/reviews", otherProductId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].userId").value(containsInAnyOrder("user-3")))
                .andExpect(jsonPath("$[0].rating").value(4));
    }

    @Test
    void returnsEmptyListWhenProductHasNoReviews() throws Exception {
        mockMvc.perform(get("/products/{id}/reviews", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsRatingOutsideOneToFive() throws Exception {
        for (String rating : List.of("0", "6", "-1", "null")) {
            submitReview(user1, productId, "{\"userId\": \"user-1\", \"rating\": " + rating + "}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("ValidationFailed"));
        }
        assertThat(reviewRepository.count()).isZero();
    }

    @Test
    void rejectsReviewWithoutUser() throws Exception {
        submitReview(user1, productId, "{\"rating\": 4}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ValidationFailed"));
    }

    @Test
    void returnsNotFoundForUnknownProduct() throws Exception {
        submitReview(user1, "unknown", "{\"userId\": \"user-1\", \"rating\": 4}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ResourceNotFound"));
        mockMvc.perform(get("/products/{id}/reviews", "unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ResourceNotFound"));
    }

    // --- access control ---

    @Test
    void readingReviewsNeedsNoToken() throws Exception {
        submitReview(user1, productId, "{\"userId\": \"user-1\", \"rating\": 5}");

        mockMvc.perform(get("/products/{id}/reviews", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void submittingReviewRequiresTheOwnersToken() throws Exception {
        String expired = TestTokens.bearer(jwtSecret, "user-1", "CUSTOMER", Instant.now().minusSeconds(60));
        String foreign = TestTokens.bearer("another-secret-that-is-long-enough-123", "user-1", "CUSTOMER");
        String json = "{\"userId\": \"user-1\", \"rating\": 5}";

        mockMvc.perform(post("/products/{id}/reviews", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AuthenticationRequired"));
        for (String badToken : List.of(expired, foreign, "Bearer not-a-token")) {
            submitReview(badToken, productId, json)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AuthenticationRequired"));
        }
        // another user's id, and an admin acting for a customer (ADMIN does not bypass the owner check)
        submitReview(user2, productId, json)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AccessDenied"));
        submitReview(admin, productId, json)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AccessDenied"));
        // the owner check comes before the product lookup: 403, not 404
        submitReview(user2, "unknown", json)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AccessDenied"));

        assertThat(reviewRepository.count()).isZero();
        assertThat(events.stream(ProductReviewed.class)).isEmpty();
    }

    @Test
    void removesReviewsWhenProductIsRemoved() throws Exception {
        submitReview(user1, productId, "{\"userId\": \"user-1\", \"rating\": 5}");
        submitReview(user2, otherProductId, "{\"userId\": \"user-2\", \"rating\": 4}");

        mockMvc.perform(delete("/products/{id}", productId).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNoContent());

        assertThat(reviewRepository.findAll()).extracting("productId").containsExactly(otherProductId);
    }

    private ResultActions submitReview(String token, String productId, String json) throws Exception {
        return mockMvc.perform(post("/products/{id}/reviews", productId)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }
}
