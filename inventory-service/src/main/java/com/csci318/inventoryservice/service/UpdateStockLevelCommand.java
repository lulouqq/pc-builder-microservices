package com.csci318.inventoryservice.service;

public record UpdateStockLevelCommand(String productId, int quantity, String reason) {
}
