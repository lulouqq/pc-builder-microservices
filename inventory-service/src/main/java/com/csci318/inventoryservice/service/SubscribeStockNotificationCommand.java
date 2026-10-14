package com.csci318.inventoryservice.service;

public record SubscribeStockNotificationCommand(String productId, String userId) {
}
