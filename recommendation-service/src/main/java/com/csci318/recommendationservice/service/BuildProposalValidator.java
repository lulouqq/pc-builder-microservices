package com.csci318.recommendationservice.service;

import com.csci318.recommendationservice.domain.CompatibilityChecker;
import com.csci318.recommendationservice.domain.CompatibilityConflict;
import com.csci318.recommendationservice.domain.ComponentType;
import com.csci318.recommendationservice.domain.SelectedComponent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Re-checks the LLM's answer for R1 (Architecture Decision 12). An answer is valid when every product ID is one
 * of the available products (so it exists in the catalog and is in stock), there is exactly one product of each
 * component type, the build passes the R2 compatibility rules, the total price from Catalog is within the budget,
 * and the description and reasoning are present. The returned problems are sent back to the LLM on the retry.
 */
class BuildProposalValidator {

    private final CompatibilityChecker compatibilityChecker = new CompatibilityChecker();

    List<String> validate(BuildProposal proposal, Map<String, CatalogProduct> availableProducts, Float budget) {
        List<String> problems = new ArrayList<>();
        if (!proposal.readable()) {
            problems.add("The answer was not a JSON object with the fields productIds, description and reasoning.");
            return problems;
        }

        Map<ComponentType, List<CatalogProduct>> selectedByType = new EnumMap<>(ComponentType.class);
        boolean allProductsAvailable = true;
        for (String productId : proposal.productIds()) {
            CatalogProduct product = availableProducts.get(productId);
            if (product == null) {
                problems.add("Product " + productId + " is not one of the available products.");
                allProductsAvailable = false;
            } else {
                selectedByType.computeIfAbsent(product.componentType().orElseThrow(), type -> new ArrayList<>())
                        .add(product);
            }
        }

        boolean onePerType = true;
        for (ComponentType type : ComponentType.values()) {
            int count = selectedByType.getOrDefault(type, List.of()).size();
            if (count == 0) {
                problems.add("No product was selected for " + type.categoryName() + ".");
                onePerType = false;
            } else if (count > 1) {
                problems.add("More than one product was selected for " + type.categoryName() + ".");
                onePerType = false;
            }
        }

        if (allProductsAvailable && onePerType) {
            List<SelectedComponent> selected = selectedByType.entrySet().stream()
                    .map(entry -> entry.getValue().get(0).toSelectedComponent(entry.getKey()))
                    .toList();
            compatibilityChecker.check(selected).conflicts().stream()
                    .map(CompatibilityConflict::message)
                    .map(message -> "Incompatible components: " + message + ".")
                    .forEach(problems::add);
        }

        float total = (float) selectedByType.values().stream()
                .flatMap(List::stream)
                .mapToDouble(CatalogProduct::price)
                .sum();
        if (total > budget) {
            problems.add("The total price " + price(total) + " is over the budget " + price(budget) + ".");
        }

        if (proposal.description() == null || proposal.description().isBlank()) {
            problems.add("The description is missing.");
        }
        if (proposal.reasoning() == null || proposal.reasoning().isBlank()) {
            problems.add("The reasoning is missing.");
        }
        return problems;
    }

    static String price(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
