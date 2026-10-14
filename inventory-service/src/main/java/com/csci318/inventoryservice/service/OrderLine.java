package com.csci318.inventoryservice.service;

/**
 * One item of a placed order: the product and the quantity to reserve.
 */
public record OrderLine(String productId, int quantity) {
}
