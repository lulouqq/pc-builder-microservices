package com.csci318.recommendationservice.service;

import java.util.List;

/**
 * What the LLM is given for R1: the request, the two preferences (kept separate; the request preference wins on
 * conflict) and the products it may choose from (in stock and in a canonical category).
 */
public record BuildContext(Float budget, String purpose, String requestPreference, String savedPreference,
                           List<CatalogProduct> availableProducts) {

    public BuildContext {
        availableProducts = List.copyOf(availableProducts);
    }
}
