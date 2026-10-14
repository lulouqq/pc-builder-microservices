package com.csci318.inventoryservice.presentation;

import com.csci318.inventoryservice.presentation.dto.StockAlertResponse;
import com.csci318.inventoryservice.service.GetStockAlertsQuery;
import com.csci318.inventoryservice.service.InventoryQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * I2: a customer's stock alerts, read by polling (Architecture Decision 17). The owner check on
 * {userId} is done by SecurityConfig.
 */
@RestController
public class StockAlertController {

    private final InventoryQueryService queryService;

    public StockAlertController(InventoryQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/users/{userId}/stock-alerts")
    public List<StockAlertResponse> getStockAlerts(@PathVariable String userId) {
        return queryService.handle(new GetStockAlertsQuery(userId)).stream()
                .map(StockAlertResponse::from)
                .toList();
    }
}
