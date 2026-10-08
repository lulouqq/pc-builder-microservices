# CQRS Command and Query Specification

> Source: `source-docs/CQRS Command and Query Specification-3.docx`

## 1. Purpose

This document defines how Inventory Service applies CQRS, including:

- Separation between command and query application services.
- Write/read model responsibilities.
- Inventory user story mapping to commands or queries.
- Command and query catalogs.
- Handler wiring and read model update flow.
- Consistency model and error model.

## 2. CQRS Principles

- Commands mutate state and execute domain invariants through InventoryItem.
- Queries read state from projections/read models and do not mutate domain state.
- Write and read models may use different schemas optimized for their workloads.
- The read side may be eventually consistent with the write side.

## 3. Application Service Split

### 3.1 Command Application Service

- Accepts command requests from API endpoints.
- Accepts commands issued by Kafka event consumers (see Section 6).
- Routes requests to command handlers.
- Loads and executes InventoryItem aggregate behaviour.
- Appends resulting events to the event store.
- Publishes events for projection updates and integrations.

### 3.2 Query Application Service

- Accepts read requests from API endpoints.
- Routes requests to query handlers.
- Reads from `inventory_read_model` only.
- Shapes response DTOs for API responses.

**Constraint**

- Queries must not mutate InventoryItem state.

## 4. Write and Read Models

### 4.1 Write Model

- Event-sourced aggregate: `InventoryItem`.

### 4.2 Read Model

- `inventory_read_model`
- Updated from InventoryItem events.
- Used for stock availability and inventory lookup queries.

## 5. User Story Classification

| User Story | Story | CQRS Type | Through Aggregate |
|---|---|---|---|
| I1 | Reserve Stock During Checkout | Command | Yes |
| I2 | Receive Stock Notifications | Command (notification subscription) + event-driven projection | No |
| I3 | Update Stock Levels | Command | Yes |

## 6. Command Catalog

| Command | Target Aggregate | Story | Output Events | Persistence |
|---|---|---|---|---|
| ReserveStockCommand | InventoryItem | I1 | StockReserved | Event store |
| ReleaseStockReservationCommand | InventoryItem | I1 | StockReservationReleased | Event store |
| ConfirmStockReservationCommand | InventoryItem | I1 | StockReservationConfirmed | Event store |
| SubscribeStockNotificationCommand | None | I2 | StockNotificationSubscribed | CRUD |
| UpdateStockLevelCommand | InventoryItem | I3 | StockLevelUpdated | Event store |

In the order flow, `ReserveStockCommand` is issued when Inventory Service consumes `OrderPlaced`, `ReleaseStockReservationCommand` when it consumes `OrderCancelled`, and `ConfirmStockReservationCommand` when it consumes `OrderCompleted`. If `ReserveStockCommand` fails with `StockUnavailable`, no event is stored and `StockReservationFailed` is published to Kafka for Order Service. See [Architecture Decisions](architecture.md#architecture-decisions).

## 7. Query Catalog

| Query | Read Source | Consistency |
|---|---|---|
| GetInventoryByProductQuery(productId) | inventory_read_model | Eventual |
| CheckStockAvailabilityQuery(productId) | inventory_read_model | Eventual |

## 8. Handler Wiring and Read Model Update Flow

Command:

```text
API → Command Application Service → Command Handler → InventoryItem → Event Store → Domain Event → Projection Handler → inventory_read_model
```

Query:

```text
API → Query Application Service → Query Handler → inventory_read_model
```

## 9. Consistency Model

- Commands are persisted immediately to the event store.
- The inventory read model is updated from inventory events.
- Queries may briefly return older data until the read model is updated.
- Therefore, the read side is eventually consistent with the write side.

## 10. Error Model

| Error | Meaning |
|---|---|
| StockUnavailable | Requested stock cannot be reserved |
| ReservationConflict | Reservation is invalid or cannot be released |
| InventoryNotFound | Inventory item does not exist |
| ValidationFailed | Invalid command or query input |

> `InventoryNotFound` maps to `ResourceNotFound` (404 Not Found) in the API error contract ([api-endpoints.md](api-endpoints.md#error-response-contract)).
