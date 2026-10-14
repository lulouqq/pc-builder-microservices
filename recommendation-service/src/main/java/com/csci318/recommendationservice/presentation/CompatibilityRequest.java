package com.csci318.recommendationservice.presentation;

import java.util.List;

/**
 * R2 request body: the IDs of the selected Catalog products.
 */
public record CompatibilityRequest(List<String> productIds) {
}
