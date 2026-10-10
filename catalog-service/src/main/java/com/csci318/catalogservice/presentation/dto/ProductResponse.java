package com.csci318.catalogservice.presentation.dto;

import com.csci318.catalogservice.domain.Product;

import java.util.List;

/**
 * stockStatus is only set when viewing product details (C2); it is null in all other responses.
 */
public record ProductResponse(String id, String name, Float price, String brand, String categoryId,
                              List<SpecificationDto> specifications, String stockStatus) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getPrice(), product.getBrand(),
                product.getCategoryId(),
                product.getSpecifications().stream().map(SpecificationDto::from).toList(),
                null);
    }
}
