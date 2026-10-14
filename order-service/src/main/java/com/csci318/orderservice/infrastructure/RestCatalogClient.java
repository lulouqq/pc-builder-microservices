package com.csci318.orderservice.infrastructure;

import com.csci318.orderservice.service.CatalogClient;
import com.csci318.orderservice.service.CatalogProduct;
import com.csci318.orderservice.service.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Reads a product's current price from Catalog Service {@code GET /products} (public) and finds the
 * product in the list. {@code GET /products/{productId}} is not used because viewing product details
 * counts towards trending products, as in Recommendation Service (Architecture Decision 12).
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
    public Optional<CatalogProduct> findProduct(String productId) {
        try {
            ProductResponse[] products = restClient.get().uri("/products").retrieve()
                    .body(ProductResponse[].class);
            if (products == null) {
                return Optional.empty();
            }
            return Arrays.stream(products)
                    .filter(product -> productId.equals(product.id()) && product.price() != null)
                    .findFirst()
                    .map(product -> new CatalogProduct(product.id(), product.name(), product.price()));
        } catch (RestClientException e) {
            log.warn("Could not get products from Catalog Service: {}", e.getMessage());
            throw new ServiceUnavailableException("Catalog Service is unavailable", e);
        }
    }

    /** Catalog's ProductResponse; only these fields are used. */
    record ProductResponse(String id, String name, Float price) {
    }
}
