package com.csci318.inventoryservice.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReadModelRepository extends JpaRepository<InventoryReadModel, String> {
}
