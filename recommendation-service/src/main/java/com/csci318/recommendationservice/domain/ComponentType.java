package com.csci318.recommendationservice.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * The canonical PC component types (domain-model.md 5.5). A product's component type is the name of its
 * Catalog category; a generated build contains exactly one product of each type.
 */
public enum ComponentType {
    CPU("CPU"),
    MOTHERBOARD("Motherboard"),
    RAM("RAM"),
    GPU("GPU"),
    STORAGE("Storage"),
    PSU("PSU"),
    CASE("Case");

    private final String categoryName;

    ComponentType(String categoryName) {
        this.categoryName = categoryName;
    }

    /** The canonical Catalog category name, also used as the componentType value in the API. */
    public String categoryName() {
        return categoryName;
    }

    /** Matches a Catalog category name, ignoring surrounding whitespace and case. */
    public static Optional<ComponentType> fromCategoryName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String trimmed = name.trim();
        return Arrays.stream(values()).filter(type -> type.categoryName.equalsIgnoreCase(trimmed)).findFirst();
    }
}
