package com.csci318.catalogservice.domain;

import com.csci318.catalogservice.domain.event.ProductViewed;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Domain service: ranks products by how often they were viewed recently (C3).
 *
 * PROVISIONAL: the calculation is a proposal waiting for team approval, and it is not connected to
 * Kafka or to an endpoint yet. The window and the list size are parameters, so no values are fixed here.
 */
public class TrendingProductsCalculator {

    /**
     * Returns the ids of the most viewed products, most viewed first.
     * Only views in the window {@code (now - window, now]} are counted. Products with the same
     * number of views are ordered by their most recent view, newest first.
     */
    public List<String> topProducts(Collection<ProductViewed> views, LocalDateTime now, Duration window, int limit) {
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }
        LocalDateTime windowStart = now.minus(window);

        Map<String, Integer> viewCounts = new HashMap<>();
        Map<String, LocalDateTime> lastViewed = new HashMap<>();
        for (ProductViewed view : views) {
            LocalDateTime occurredAt = view.occurredAt();
            if (occurredAt.isAfter(windowStart) && !occurredAt.isAfter(now)) {
                viewCounts.merge(view.productId(), 1, Integer::sum);
                lastViewed.merge(view.productId(), occurredAt, (a, b) -> a.isAfter(b) ? a : b);
            }
        }

        return viewCounts.keySet().stream()
                .sorted(Comparator.<String, Integer>comparing(viewCounts::get).reversed()
                        .thenComparing(lastViewed::get, Comparator.reverseOrder())
                        .thenComparing(Comparator.naturalOrder()))
                .limit(limit)
                .toList();
    }
}
