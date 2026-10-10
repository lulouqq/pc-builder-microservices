package com.csci318.catalogservice.domain.event;

import com.csci318.catalogservice.domain.Product;

import java.time.LocalDateTime;

/**
 * Domain event (internal to Catalog Service): a product was created.
 */
public record ProductCreated(String productId, String name, Float price, String brand, String categoryId,
                             LocalDateTime occurredAt) {

    public static ProductCreated of(Product product) {
        return new ProductCreated(product.getId(), product.getName(), product.getPrice(), product.getBrand(),
                product.getCategoryId(), LocalDateTime.now());
    }
}
