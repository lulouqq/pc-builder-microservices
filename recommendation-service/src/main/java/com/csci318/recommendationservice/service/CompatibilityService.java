package com.csci318.recommendationservice.service;

import com.csci318.recommendationservice.domain.CompatibilityChecker;
import com.csci318.recommendationservice.domain.CompatibilityResult;
import com.csci318.recommendationservice.domain.ComponentType;
import com.csci318.recommendationservice.domain.SelectedComponent;
import com.csci318.recommendationservice.domain.ValidationException;
import com.csci318.recommendationservice.domain.event.CompatibilityChecked;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Application service for R2: deterministic compatibility check of customer-selected products.
 * Does not use the LLM and stores nothing.
 */
@Service
public class CompatibilityService {

    private final CatalogClient catalogClient;
    private final ApplicationEventPublisher eventPublisher;
    private final CompatibilityChecker compatibilityChecker = new CompatibilityChecker();

    public CompatibilityService(CatalogClient catalogClient, ApplicationEventPublisher eventPublisher) {
        this.catalogClient = catalogClient;
        this.eventPublisher = eventPublisher;
    }

    /**
     * @throws ValidationException        empty list, blank id, two products of one type, or a non-canonical product
     * @throws ResourceNotFoundException  a product id is not in the catalog
     */
    public CompatibilityResult validate(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            throw new ValidationException("productIds must contain at least one product id");
        }
        if (productIds.stream().anyMatch(id -> id == null || id.isBlank())) {
            throw new ValidationException("productIds must not contain blank ids");
        }

        Map<String, CatalogProduct> catalog = catalogClient.getProducts().stream()
                .collect(Collectors.toMap(CatalogProduct::id, Function.identity(), (first, second) -> first));
        for (String productId : productIds) {
            if (!catalog.containsKey(productId)) {
                throw new ResourceNotFoundException("Product not found: " + productId);
            }
        }

        List<SelectedComponent> selected = new ArrayList<>();
        Set<ComponentType> seenTypes = EnumSet.noneOf(ComponentType.class);
        for (String productId : productIds) {
            CatalogProduct product = catalog.get(productId);
            ComponentType type = product.componentType().orElseThrow(() -> new ValidationException(
                    "Product " + productId + " is not in a canonical component category"
                            + " (CPU, Motherboard, RAM, GPU, Storage, PSU, Case)"));
            if (!seenTypes.add(type)) {
                throw new ValidationException("Only one product per component type can be checked; "
                        + "more than one was given for " + type.categoryName());
            }
            selected.add(product.toSelectedComponent(type));
        }

        CompatibilityResult result = compatibilityChecker.check(selected);
        eventPublisher.publishEvent(CompatibilityChecked.of(productIds, result));
        return result;
    }
}
