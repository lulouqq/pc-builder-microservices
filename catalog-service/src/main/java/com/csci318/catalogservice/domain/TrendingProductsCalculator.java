package com.csci318.catalogservice.domain;

import com.csci318.catalogservice.domain.event.ProductViewed;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Domain service: ranks products by how often they were viewed recently (C3, Architecture Decision 16).
 * The window and the list size are parameters; their values come from configuration.
 */
public class TrendingProductsCalculator {

    /**
     * Returns the ids of the most viewed products, most viewed first.
     * Only views in the window {@code (now - window, now]} are counted. Products with the same
     * number of views are ordered by their most recent view, newest first, then by product id.
     */
    public List<String> topProducts(Collection<ProductViewed> views, Instant now, Duration window, int limit) {
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }
        Instant windowStart = now.minus(window);

        Map<String, Integer> viewCounts = new HashMap<>();
        Map<String, Instant> lastViewed = new HashMap<>();
        for (ProductViewed view : views) {
            Instant occurredAt = view.occurredAt();
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
