package com.csci318.recommendationservice.service;

import java.util.List;

/**
 * Port to Catalog Service (public product and category endpoints).
 */
public interface CatalogClient {

    /**
     * All catalog products, each with the name of its category.
     *
     * @throws ServiceUnavailableException when Catalog Service is unavailable or fails
     */
    List<CatalogProduct> getProducts();
}
