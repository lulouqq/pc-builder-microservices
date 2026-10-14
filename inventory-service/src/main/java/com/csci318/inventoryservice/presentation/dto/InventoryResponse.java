package com.csci318.inventoryservice.presentation.dto;

import com.csci318.inventoryservice.infrastructure.InventoryReadModel;

/**
 * Contract used by Catalog Service (C2) and Recommendation Service (R1): available stock is
 * quantity - reservedQuantity.
 */
public record InventoryResponse(String productId, int quantity, int reservedQuantity) {

    public static InventoryResponse from(InventoryReadModel readModel) {
        return new InventoryResponse(readModel.getProductId(), readModel.getQuantity(),
                readModel.getReservedQuantity());
    }
}
