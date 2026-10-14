package com.csci318.recommendationservice.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompatibilityCheckerTest {

    private final CompatibilityChecker checker = new CompatibilityChecker();

    @Test
    void matchingSocketMemoryTypeAndFormFactorAreCompatible() {
        CompatibilityResult result = checker.check(List.of(
                component("cpu", ComponentType.CPU, "Socket", "AM5"),
                motherboard("AM5", "DDR5", "ATX"),
                component("ram", ComponentType.RAM, "Memory Type", "DDR5"),
                component("case", ComponentType.CASE, "Form Factor", "ATX")));

        assertThat(result.compatible()).isTrue();
        assertThat(result.conflicts()).isEmpty();
    }

    @Test
    void differentSocketIsAConflict() {
        CompatibilityResult result = checker.check(List.of(
                component("cpu", ComponentType.CPU, "Socket", "LGA1700"),
                motherboard("AM5", "DDR5", "ATX")));

        assertThat(result.compatible()).isFalse();
        assertThat(result.conflicts()).singleElement().satisfies(conflict -> {
            assertThat(conflict.specName()).isEqualTo("Socket");
            assertThat(conflict.productIds()).containsExactly("cpu", "mb");
            assertThat(conflict.message()).contains("LGA1700").contains("AM5");
        });
    }

    @Test
    void differentMemoryTypeAndFormFactorAreConflicts() {
        CompatibilityResult result = checker.check(List.of(
                motherboard("AM5", "DDR5", "ATX"),
                component("ram", ComponentType.RAM, "Memory Type", "DDR4"),
                component("case", ComponentType.CASE, "Form Factor", "Mini-ITX")));

        assertThat(result.conflicts()).extracting(CompatibilityConflict::specName)
                .containsExactly("Memory Type", "Form Factor");
    }

    @Test
    void ruleIsSkippedWhenOneOfItsComponentTypesIsNotSelected() {
        CompatibilityResult result = checker.check(List.of(
                component("cpu", ComponentType.CPU, "Socket", "AM5"),
                component("ram", ComponentType.RAM, "Memory Type", "DDR4"),
                component("gpu", ComponentType.GPU, "Memory", "8GB")));

        assertThat(result.compatible()).isTrue();
    }

    @Test
    void missingSpecificationIsAConflict() {
        CompatibilityResult result = checker.check(List.of(
                new SelectedComponent("mb", "Board", ComponentType.MOTHERBOARD, List.of(new Specification("Socket", "AM5"))),
                component("ram", ComponentType.RAM, "Memory Type", "DDR5")));

        assertThat(result.compatible()).isFalse();
        assertThat(result.conflicts()).singleElement().satisfies(conflict -> {
            assertThat(conflict.specName()).isEqualTo("Memory Type");
            assertThat(conflict.productIds()).containsExactly("ram", "mb");
            assertThat(conflict.message()).contains("Motherboard 'Board' has no 'Memory Type' specification");
        });
    }

    @Test
    void blankSpecificationValueCountsAsMissing() {
        CompatibilityResult result = checker.check(List.of(
                component("cpu", ComponentType.CPU, "Socket", "  "),
                motherboard("AM5", "DDR5", "ATX")));

        assertThat(result.conflicts()).singleElement()
                .satisfies(conflict -> assertThat(conflict.message()).contains("has no 'Socket' specification"));
    }

    @Test
    void valuesAndNamesAreComparedTrimmedAndIgnoringCase() {
        CompatibilityResult result = checker.check(List.of(
                component("cpu", ComponentType.CPU, " socket ", " am5 "),
                motherboard("AM5", "DDR5", "ATX")));

        assertThat(result.compatible()).isTrue();
    }

    @Test
    void valuesAreNotInterpreted() {
        CompatibilityResult result = checker.check(List.of(
                motherboard("AM5", "DDR5", "Micro-ATX"),
                component("case", ComponentType.CASE, "Form Factor", "ATX")));

        assertThat(result.compatible()).isFalse();
    }

    @Test
    void twoComponentsOfTheSameTypeAreRejected() {
        assertThatThrownBy(() -> checker.check(List.of(
                component("cpu1", ComponentType.CPU, "Socket", "AM5"),
                component("cpu2", ComponentType.CPU, "Socket", "AM5"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void componentTypeIsMatchedFromCanonicalCategoryNames() {
        assertThat(ComponentType.fromCategoryName("Motherboard")).contains(ComponentType.MOTHERBOARD);
        assertThat(ComponentType.fromCategoryName(" psu ")).contains(ComponentType.PSU);
        assertThat(ComponentType.fromCategoryName("Cooler")).isEmpty();
        assertThat(ComponentType.fromCategoryName(null)).isEmpty();
    }

    private static SelectedComponent component(String id, ComponentType type, String spec, String value) {
        return new SelectedComponent(id, null, type, List.of(new Specification(spec, value)));
    }

    private static SelectedComponent motherboard(String socket, String memoryType, String formFactor) {
        return new SelectedComponent("mb", "Board", ComponentType.MOTHERBOARD, List.of(
                new Specification("Socket", socket),
                new Specification("Memory Type", memoryType),
                new Specification("Form Factor", formFactor)));
    }
}
