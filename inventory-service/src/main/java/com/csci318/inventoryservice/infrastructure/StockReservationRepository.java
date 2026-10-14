package com.csci318.inventoryservice.infrastructure;

import com.csci318.inventoryservice.domain.ReservationStatus;
import com.csci318.inventoryservice.domain.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockReservationRepository extends JpaRepository<StockReservation, String> {

    boolean existsByOrderId(String orderId);

    List<StockReservation> findByOrderIdAndStatus(String orderId, ReservationStatus status);
}
