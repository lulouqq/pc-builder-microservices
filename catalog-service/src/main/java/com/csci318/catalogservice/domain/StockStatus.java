package com.csci318.catalogservice.domain;

/**
 * Current stock status of a product, as shown in product details (C2).
 * UNKNOWN means no stock information could be obtained; it is not the same as OUT_OF_STOCK.
 */
public enum StockStatus {
    IN_STOCK,
    OUT_OF_STOCK,
    UNKNOWN;

    public static StockStatus ofAvailableQuantity(int availableQuantity) {
        return availableQuantity > 0 ? IN_STOCK : OUT_OF_STOCK;
    }
}
