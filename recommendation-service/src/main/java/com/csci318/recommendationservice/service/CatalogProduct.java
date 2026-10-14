package com.csci318.recommendationservice.service;

import com.csci318.recommendationservice.domain.ComponentType;
import com.csci318.recommendationservice.domain.SelectedComponent;
import com.csci318.recommendationservice.domain.Specification;

import java.util.List;
import java.util.Optional;

/**
 * A product as read from Catalog Service, with the name of its category (null if the category is unknown).
 */
public record CatalogProduct(String id, String name, String brand, Float price, String categoryName,
                             List<Specification> specifications) {

    public CatalogProduct {
        specifications = specifications == null ? List.of() : List.copyOf(specifications);
    }

    /** The canonical component type, when the product's category is one of the canonical categories. */
    public Optional<ComponentType> componentType() {
        return ComponentType.fromCategoryName(categoryName);
    }

    SelectedComponent toSelectedComponent(ComponentType type) {
        return new SelectedComponent(id, name, type, specifications);
    }
}
