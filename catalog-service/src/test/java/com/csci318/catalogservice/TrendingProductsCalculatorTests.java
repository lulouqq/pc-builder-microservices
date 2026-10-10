package com.csci318.catalogservice;

import com.csci318.catalogservice.domain.TrendingProductsCalculator;
import com.csci318.catalogservice.domain.event.ProductViewed;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * C3: trending calculation, tested with sample ProductViewed events (no Kafka involved).
 */
class TrendingProductsCalculatorTests {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 10, 12, 0);
    private static final Duration ONE_HOUR = Duration.ofHours(1);

    private final TrendingProductsCalculator calculator = new TrendingProductsCalculator();

    @Test
    void ranksProductsByNumberOfViews() {
        List<ProductViewed> views = List.of(
                viewed("cpu", 5), viewed("gpu", 6), viewed("gpu", 7), viewed("ram", 8),
                viewed("gpu", 9), viewed("cpu", 10));

        assertThat(calculator.topProducts(views, NOW, ONE_HOUR, 10)).containsExactly("gpu", "cpu", "ram");
    }

    @Test
    void countsOnlyViewsInsideTheWindow() {
        List<ProductViewed> views = List.of(
                viewed("old", 61), viewed("old", 90), viewed("old", 120),
                viewed("edge", 60),
                viewed("recent", 59),
                new ProductViewed("future", NOW.plusMinutes(1)));

        // the window is (now - 1h, now]: a view exactly one hour old is outside it
        assertThat(calculator.topProducts(views, NOW, ONE_HOUR, 10)).containsExactly("recent");
        assertThat(calculator.topProducts(views, NOW, Duration.ofHours(3), 10))
                .containsExactly("old", "recent", "edge");
    }

    @Test
    void includesViewAtExactlyNow() {
        assertThat(calculator.topProducts(List.of(viewed("cpu", 0)), NOW, ONE_HOUR, 10)).containsExactly("cpu");
    }

    @Test
    void breaksTiesByMostRecentView() {
        List<ProductViewed> views = List.of(
                viewed("cpu", 30), viewed("cpu", 20),
                viewed("gpu", 40), viewed("gpu", 5),
                viewed("ram", 50), viewed("ram", 45));

        assertThat(calculator.topProducts(views, NOW, ONE_HOUR, 10)).containsExactly("gpu", "cpu", "ram");
    }

    @Test
    void returnsAtMostLimitProducts() {
        List<ProductViewed> views = List.of(
                viewed("cpu", 1), viewed("cpu", 2), viewed("cpu", 3),
                viewed("gpu", 4), viewed("gpu", 5),
                viewed("ram", 6));

        assertThat(calculator.topProducts(views, NOW, ONE_HOUR, 2)).containsExactly("cpu", "gpu");
    }

    @Test
    void returnsEmptyListWhenThereAreNoRecentViews() {
        assertThat(calculator.topProducts(List.of(), NOW, ONE_HOUR, 10)).isEmpty();
        assertThat(calculator.topProducts(List.of(viewed("cpu", 500)), NOW, ONE_HOUR, 10)).isEmpty();
    }

    @Test
    void rejectsInvalidWindowOrLimit() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> calculator.topProducts(List.of(), NOW, Duration.ZERO, 10));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> calculator.topProducts(List.of(), NOW, ONE_HOUR, 0));
    }

    private ProductViewed viewed(String productId, int minutesAgo) {
        return new ProductViewed(productId, NOW.minusMinutes(minutesAgo));
    }
}
