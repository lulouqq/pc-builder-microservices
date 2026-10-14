package com.csci318.recommendationservice.presentation;

import com.csci318.recommendationservice.domain.BuildComponent;

/**
 * One component of a recommended build; componentType is the canonical category name (e.g. "Motherboard").
 */
public record ComponentResponse(String productId, String componentType, Float price) {

    public static ComponentResponse from(BuildComponent component) {
        return new ComponentResponse(component.getProductId(), component.getComponentType().categoryName(),
                component.getPrice());
    }
}
