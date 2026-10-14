package com.csci318.catalogservice.service;

import com.csci318.catalogservice.domain.Product;
import com.csci318.catalogservice.domain.TrendingProductsCalculator;
import com.csci318.catalogservice.domain.event.ProductViewed;
import com.csci318.catalogservice.infrastructure.ProductRepository;
import com.csci318.catalogservice.infrastructure.ProductViewRepository;
import com.csci318.catalogservice.infrastructure.StoredProductView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Trending products (C3, Architecture Decision 16): records product views and ranks products by the
 * number of views in the configured window.
 */
@Service
public class TrendingService {

    private final ProductViewRepository viewRepository;
    private final ProductRepository productRepository;
    private final Duration window;
    private final int size;
    private final TrendingProductsCalculator calculator = new TrendingProductsCalculator();

    public TrendingService(ProductViewRepository viewRepository, ProductRepository productRepository,
                           @Value("${catalog.trending.window}") Duration window,
                           @Value("${catalog.trending.size}") int size) {
        if (window.isZero() || window.isNegative() || size < 1) {
            throw new IllegalStateException(
                    "catalog.trending.window must be positive and catalog.trending.size at least 1");
        }
        this.viewRepository = viewRepository;
        this.productRepository = productRepository;
        this.window = window;
        this.size = size;
    }

    /**
     * Stores one view. A view whose event id is already stored (a repeated message) is skipped.
     * Runs in its own transaction because it is called after the C2 transaction has committed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordView(String eventId, ProductViewed view) {
        if (!viewRepository.existsById(eventId)) {
            viewRepository.save(new StoredProductView(eventId, view.productId(), view.occurredAt()));
        }
    }

    /**
     * The most viewed products of the window, most viewed first. Removed products are left out, so the
     * list can be shorter than the configured size, or empty.
     */
    @Transactional(readOnly = true)
    public List<Product> getTrendingProducts() {
        Instant now = Instant.now();
        List<ProductViewed> views = viewRepository.findByOccurredAtAfter(now.minus(window)).stream()
                .map(StoredProductView::toEvent)
                .toList();
        if (views.isEmpty()) {
            return List.of();
        }
        // rank every viewed product, so that removed ones can be skipped without shortening the list
        List<String> ranked = calculator.topProducts(views, now, window, Integer.MAX_VALUE);
        Map<String, Product> products = productRepository.findAllById(ranked).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        return ranked.stream()
                .filter(products::containsKey)
                .map(products::get)
                .limit(size)
                .toList();
    }
}
