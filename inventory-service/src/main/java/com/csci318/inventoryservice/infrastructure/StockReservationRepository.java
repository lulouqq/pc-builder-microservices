package com.csci318.inventoryservice.infrastructure;

import com.csci318.inventoryservice.domain.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockReservationRepository extends JpaRepository<StockReservation, String> {
}
