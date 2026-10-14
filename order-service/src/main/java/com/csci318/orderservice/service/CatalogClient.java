package com.csci318.orderservice.service;

import java.util.Optional;

/**
 * Inter-service query to Catalog Service for a product and its current price.
 */
public interface CatalogClient {

    /**
     * Empty when the product is not in the catalog.
     *
     * @throws ServiceUnavailableException when Catalog Service cannot be reached
     */
    Optional<CatalogProduct> findProduct(String productId);
}
