package com.csci318.recommendationservice.domain;

import java.util.List;
import java.util.Optional;

/**
 * Value object: a product selected for a compatibility check, with its component type and specifications.
 */
public record SelectedComponent(String productId, String productName, ComponentType componentType,
                                List<Specification> specifications) {

    public SelectedComponent {
        specifications = specifications == null ? List.of() : List.copyOf(specifications);
    }

    /**
     * The trimmed value of the first specification with this name (name matched ignoring surrounding
     * whitespace and case), or empty when the product has no non-blank value for it.
     */
    public Optional<String> specificationValue(String name) {
        return specifications.stream()
                .filter(spec -> spec.name() != null && spec.name().trim().equalsIgnoreCase(name))
                .map(Specification::value)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .findFirst();
    }

    String label() {
        return componentType.categoryName() + " '" + (productName == null ? productId : productName) + "'";
    }
}
