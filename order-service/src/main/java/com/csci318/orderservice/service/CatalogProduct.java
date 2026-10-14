package com.csci318.orderservice.service;

/**
 * A product as read from Catalog Service.
 */
public record CatalogProduct(String id, String name, Float price) {
}
