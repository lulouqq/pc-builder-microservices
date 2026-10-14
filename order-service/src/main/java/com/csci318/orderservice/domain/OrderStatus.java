package com.csci318.orderservice.domain;

/**
 * Order lifecycle (specs/domain-model.md, 3.3): DRAFT (the cart) -> PLACED -> PROCESSING -> COMPLETED,
 * or CANCELLED from DRAFT, PLACED or PROCESSING.
 */
public enum OrderStatus {
    DRAFT,
    PLACED,
    PROCESSING,
    COMPLETED,
    CANCELLED
}
