package com.csci318.recommendationservice.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Domain service: checks selected components against the {@link CompatibilityRule}s (used by R1 and R2).
 * <ul>
 *   <li>A rule is skipped when one of its two component types is not selected.</li>
 *   <li>When both are selected but a product has no value for the rule's specification, it is a conflict.</li>
 *   <li>Values are compared after trimming, ignoring case; they are not interpreted otherwise.</li>
 * </ul>
 */
public class CompatibilityChecker {

    /**
     * @throws IllegalArgumentException when more than one component of the same type is given
     */
    public CompatibilityResult check(Collection<SelectedComponent> components) {
        Map<ComponentType, SelectedComponent> byType = new EnumMap<>(ComponentType.class);
        for (SelectedComponent component : components) {
            if (byType.putIfAbsent(component.componentType(), component) != null) {
                throw new IllegalArgumentException(
                        "Only one product per component type can be checked: " + component.componentType());
            }
        }

        List<CompatibilityConflict> conflicts = new ArrayList<>();
        for (CompatibilityRule rule : CompatibilityRule.values()) {
            SelectedComponent first = byType.get(rule.first());
            SelectedComponent second = byType.get(rule.second());
            if (first != null && second != null) {
                checkRule(rule, first, second).ifPresent(conflicts::add);
            }
        }
        return CompatibilityResult.of(conflicts);
    }

    private Optional<CompatibilityConflict> checkRule(CompatibilityRule rule, SelectedComponent first,
                                                      SelectedComponent second) {
        String spec = rule.specificationName();
        Optional<String> firstValue = first.specificationValue(spec);
        Optional<String> secondValue = second.specificationValue(spec);

        String message;
        if (firstValue.isEmpty() || secondValue.isEmpty()) {
            List<String> missing = new ArrayList<>();
            if (firstValue.isEmpty()) {
                missing.add(first.label());
            }
            if (secondValue.isEmpty()) {
                missing.add(second.label());
            }
            message = String.join(" and ", missing) + (missing.size() == 1 ? " has" : " have")
                    + " no '" + spec + "' specification, so "
                    + first.label() + " and " + second.label() + " cannot be checked for compatibility";
        } else if (!firstValue.get().equalsIgnoreCase(secondValue.get())) {
            message = spec + " does not match: " + first.label() + " has '" + firstValue.get() + "' but "
                    + second.label() + " has '" + secondValue.get() + "'";
        } else {
            return Optional.empty();
        }
        return Optional.of(new CompatibilityConflict(spec, List.of(first.productId(), second.productId()), message));
    }
}
