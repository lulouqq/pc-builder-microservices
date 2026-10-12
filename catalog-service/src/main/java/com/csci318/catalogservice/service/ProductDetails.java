package com.csci318.catalogservice.service;

import com.csci318.catalogservice.domain.Product;
import com.csci318.catalogservice.domain.StockStatus;

/**
 * A product together with its current stock status from Inventory Service (C2).
 */
public record ProductDetails(Product product, StockStatus stockStatus) {
}
