package com.csci318.inventoryservice.domain;

/**
 * Why a stock alert was created: the StockChanged change that notifies subscribers (I2).
 */
public enum StockAlertType {
    STOCK_LOW,
    PRODUCT_RESTOCKED
}
