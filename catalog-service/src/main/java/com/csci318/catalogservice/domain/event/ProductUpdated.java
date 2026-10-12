package com.csci318.catalogservice.domain.event;

import com.csci318.catalogservice.domain.Product;

import java.time.LocalDateTime;

/**
 * Domain event (internal to Catalog Service): a product was updated.
 */
public record ProductUpdated(String productId, String name, Float price, String brand, String categoryId,
                             LocalDateTime occurredAt) {

    public static ProductUpdated of(Product product) {
        return new ProductUpdated(product.getId(), product.getName(), product.getPrice(), product.getBrand(),
                product.getCategoryId(), LocalDateTime.now());
    }
}
