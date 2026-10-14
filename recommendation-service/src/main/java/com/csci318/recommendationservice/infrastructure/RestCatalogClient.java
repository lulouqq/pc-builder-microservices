package com.csci318.recommendationservice.infrastructure;

import com.csci318.recommendationservice.domain.Specification;
import com.csci318.recommendationservice.service.CatalogClient;
import com.csci318.recommendationservice.service.CatalogProduct;
import com.csci318.recommendationservice.service.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Calls Catalog Service {@code GET /categories} and {@code GET /products} (both public) and joins them, so that
 * each product carries its category name. {@code GET /products/{productId}} is deliberately not used
 * (Architecture Decision 12).
 */
@Component
public class RestCatalogClient implements CatalogClient {

    private static final Logger log = LoggerFactory.getLogger(RestCatalogClient.class);

    private final RestClient restClient;

    public RestCatalogClient(RestClient.Builder builder,
                             @Value("${catalog-service.base-url}") String baseUrl,
                             @Value("${catalog-service.timeout}") Duration timeout) {
        this.restClient = RestClients.create(builder, baseUrl, timeout);
    }

    @Override
    public List<CatalogProduct> getProducts() {
        try {
            CategoryResponse[] categories = restClient.get().uri("/categories").retrieve()
                    .body(CategoryResponse[].class);
            ProductResponse[] products = restClient.get().uri("/products").retrieve()
                    .body(ProductResponse[].class);
            Map<String, String> categoryNames = categories == null ? Map.of() : Arrays.stream(categories)
                    .collect(Collectors.toMap(CategoryResponse::id, CategoryResponse::name, (first, second) -> first));
            return products == null ? List.of() : Arrays.stream(products)
                    .map(product -> product.toCatalogProduct(categoryNames.get(product.categoryId())))
                    .toList();
        } catch (RestClientException e) {
            log.warn("Could not get products from Catalog Service: {}", e.getMessage());
            throw new ServiceUnavailableException("Catalog Service is unavailable", e);
        }
    }

    record CategoryResponse(String id, String name) {
    }

    record SpecificationResponse(String name, String value) {
    }

    /** Catalog's ProductResponse; stockStatus is not used (it is null in lists). */
    record ProductResponse(String id, String name, Float price, String brand, String categoryId,
                           List<SpecificationResponse> specifications) {

        CatalogProduct toCatalogProduct(String categoryName) {
            List<Specification> specs = specifications == null ? List.of() : specifications.stream()
                    .map(spec -> new Specification(spec.name(), spec.value()))
                    .toList();
            return new CatalogProduct(id, name, brand, price, categoryName, specs);
        }
    }
}
