# Event Sourcing Specification

> Source: `source-docs/Event Sourcing Specification-2.docx`

## 1. Scope

- This specification defines event sourcing for Inventory Service, where only `InventoryItem` is event-sourced.
- Other aggregates in the system remain CRUD-based.

## 2. Goals

- Preserve inventory state transitions as immutable events.
- Support current stock availability queries through a projected read model.
- Provide a replayable stock history for auditing and analysis.
- Support real-time stock monitoring and reservation workflows.

## 3. Event Model

### 3.1 Event Fields

| Field | Type | Description |
|---|---|---|
| eventId | UUID | Unique event identity. Used for idempotency/deduplication. |
| aggregateId | String | Identity of the InventoryItem (`InventoryItem.id`). The product is identified separately by `productId` in the event payload. |
| aggregateType | String | Always "InventoryItem" |
| eventType | String | One of the event types defined below |
| aggregateVersion | long | Increasing version of the aggregate stream |
| occurredAt | timestamp | Time the event occurred |
| payload | JSON | Event-specific state change data |

### 3.2 Event Stream Identity

Stream key = `aggregateType + aggregateId`

Example: `InventoryItem:inventory-item-123`

### 3.3 Event Types

| Event Type | Triggering Behaviour | Key Payload Fields |
|---|---|---|
| InventoryInitialized | first creation of inventory record | productId, quantity, reservedQuantity |
| StockReserved | reserveStock(quantity, orderId) | productId, orderId, reservationId, quantity |
| StockReservationReleased | releaseReservation(reservationId) | productId, orderId, reservationId, quantity |
| StockReservationConfirmed | confirmReservation(reservationId) | productId, orderId, reservationId, quantity |
| StockLevelUpdated | updateStock(quantity) | productId, previousQuantity, newQuantity, reason |

> Quantity effects: `StockReserved` increases `reservedQuantity`; `StockReservationReleased` decreases `reservedQuantity`; `StockReservationConfirmed` decreases both `reservedQuantity` and `quantity` (the reserved stock is sold).

> `InventoryInitialized` is created internally when an inventory record is first created for a product; it is not triggered by an API endpoint or a CQRS command.

### 3.4 Derived Events

- `StockLow` - Triggered when available stock falls below the configured threshold.
- `ProductRestocked` - Triggered when stock becomes available again after previously being unavailable.

> The `StockChanged` event shown in the [architecture diagram](architecture.md#kafka-flows) is an integration/umbrella event representing inventory stock changes; it may be derived from `StockLevelUpdated`, `StockLow` or `ProductRestocked`.
