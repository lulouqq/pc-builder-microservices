package com.csci318.inventoryservice.service;

public record ReserveStockCommand(String productId, String orderId, int quantity) {
}
