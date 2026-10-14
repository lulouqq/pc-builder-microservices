package com.csci318.recommendationservice.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BuildAggregatesTest {

    @Test
    void buildRequestKeepsRequestDataAndClearsBlankPreferences() {
        BuildRequest request = new BuildRequest("user-1", 1500f, " gaming ", "  ");

        assertThat(request.getId()).isNotBlank();
        assertThat(request.getUserId()).isEqualTo("user-1");
        assertThat(request.getBudget()).isEqualTo(1500f);
        assertThat(request.getPurpose()).isEqualTo("gaming");
        assertThat(request.getPreferences()).isNull();
    }

    @Test
    void buildRequestRequiresPositiveBudgetAndPurpose() {
        assertThatThrownBy(() -> new BuildRequest("user-1", 0f, "gaming", null))
                .isInstanceOf(ValidationException.class).hasMessageContaining("budget");
        assertThatThrownBy(() -> new BuildRequest("user-1", -5f, "gaming", null))
                .isInstanceOf(ValidationException.class).hasMessageContaining("budget");
        assertThatThrownBy(() -> new BuildRequest("user-1", null, "gaming", null))
                .isInstanceOf(ValidationException.class).hasMessageContaining("budget");
        assertThatThrownBy(() -> new BuildRequest("user-1", 1000f, " ", null))
                .isInstanceOf(ValidationException.class).hasMessageContaining("purpose");
        assertThatThrownBy(() -> new BuildRequest(null, 1000f, "gaming", null))
                .isInstanceOf(ValidationException.class).hasMessageContaining("userId");
    }

    @Test
    void pcBuildTotalIsTheSumOfComponentPrices() {
        PCBuild build = new PCBuild("request-1", "Gaming build", List.of(
                new BuildComponent("cpu", ComponentType.CPU, 200f),
                new BuildComponent("gpu", ComponentType.GPU, 300.5f)));

        assertThat(build.getTotalPrice()).isEqualTo(500.5f);
        assertThat(build.getComponents()).extracting(BuildComponent::getProductId).containsExactly("cpu", "gpu");
    }

    @Test
    void pcBuildHasAtMostOneRecommendation() {
        PCBuild build = new PCBuild("request-1", "Gaming build",
                List.of(new BuildComponent("cpu", ComponentType.CPU, 200f)));

        Recommendation recommendation = build.recommend("Good value");

        assertThat(recommendation.getBuildId()).isEqualTo(build.getId());
        assertThat(build.getRecommendation()).isSameAs(recommendation);
        assertThatThrownBy(() -> build.recommend("Again")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void pcBuildNeedsComponentsAndDescription() {
        assertThatThrownBy(() -> new PCBuild("request-1", "Gaming build", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PCBuild("request-1", " ",
                List.of(new BuildComponent("cpu", ComponentType.CPU, 200f))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
