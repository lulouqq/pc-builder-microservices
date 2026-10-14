package com.csci318.recommendationservice.domain;

/**
 * The deterministic compatibility rules (domain-model.md 5.5): two component types must have the same
 * value for a specification. There is no power (wattage) rule.
 */
public enum CompatibilityRule {
    CPU_MOTHERBOARD_SOCKET(ComponentType.CPU, ComponentType.MOTHERBOARD, "Socket"),
    RAM_MOTHERBOARD_MEMORY_TYPE(ComponentType.RAM, ComponentType.MOTHERBOARD, "Memory Type"),
    MOTHERBOARD_CASE_FORM_FACTOR(ComponentType.MOTHERBOARD, ComponentType.CASE, "Form Factor");

    private final ComponentType first;
    private final ComponentType second;
    private final String specificationName;

    CompatibilityRule(ComponentType first, ComponentType second, String specificationName) {
        this.first = first;
        this.second = second;
        this.specificationName = specificationName;
    }

    public ComponentType first() {
        return first;
    }

    public ComponentType second() {
        return second;
    }

    public String specificationName() {
        return specificationName;
    }
}
