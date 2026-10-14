package com.csci318.inventoryservice.presentation;

import com.csci318.inventoryservice.presentation.dto.InventoryResponse;
import com.csci318.inventoryservice.presentation.dto.StockNotificationRequest;
import com.csci318.inventoryservice.presentation.dto.StockNotificationResponse;
import com.csci318.inventoryservice.presentation.dto.StockReservationRequest;
import com.csci318.inventoryservice.presentation.dto.StockReservationResponse;
import com.csci318.inventoryservice.presentation.dto.StockUpdateRequest;
import com.csci318.inventoryservice.service.GetInventoryByProductQuery;
import com.csci318.inventoryservice.service.InventoryCommandService;
import com.csci318.inventoryservice.service.InventoryQueryService;
import com.csci318.inventoryservice.service.ReleaseStockReservationCommand;
import com.csci318.inventoryservice.service.ReserveStockCommand;
import com.csci318.inventoryservice.service.SubscribeStockNotificationCommand;
import com.csci318.inventoryservice.service.UpdateStockLevelCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Commands go to the command service and reads go to the query service (CQRS).
 */
@RestController
@RequestMapping("/inventory/{productId}")
public class InventoryController {

    private final InventoryCommandService commandService;
    private final InventoryQueryService queryService;

    public InventoryController(InventoryCommandService commandService, InventoryQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping
    public InventoryResponse getInventory(@PathVariable String productId) {
        return InventoryResponse.from(queryService.handle(new GetInventoryByProductQuery(productId)));
    }

    @PutMapping
    public InventoryResponse updateStockLevel(@PathVariable String productId,
                                              @Valid @RequestBody StockUpdateRequest request) {
        commandService.handle(new UpdateStockLevelCommand(productId, request.quantity(), request.reason()));
        return InventoryResponse.from(queryService.handle(new GetInventoryByProductQuery(productId)));
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public StockReservationResponse reserveStock(@PathVariable String productId,
                                                 @Valid @RequestBody StockReservationRequest request) {
        String reservationId = commandService.handle(
                new ReserveStockCommand(productId, request.orderId(), request.quantity()));
        return StockReservationResponse.from(queryService.getReservation(reservationId));
    }

    @DeleteMapping("/reservations/{reservationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void releaseReservation(@PathVariable String productId, @PathVariable String reservationId) {
        commandService.handle(new ReleaseStockReservationCommand(productId, reservationId));
    }

    @PostMapping("/notifications")
    @ResponseStatus(HttpStatus.CREATED)
    public StockNotificationResponse subscribe(@PathVariable String productId,
                                               @Valid @RequestBody StockNotificationRequest request,
                                               @AuthenticationPrincipal Jwt token) {
        // owner check: customers can only subscribe themselves (checked before the product is looked up)
        if (!token.getSubject().equals(request.userId())) {
            throw new AccessDeniedException("Token does not belong to user " + request.userId());
        }
        return StockNotificationResponse.from(
                commandService.handle(new SubscribeStockNotificationCommand(productId, request.userId())));
    }
}
