# inventory-service

Responsibilities: stock, inventory items, stock reservations, stock notification subscriptions.

Port: 8082

## Status

| Story | Status |
|---|---|
| I1 Reserve Stock During Checkout | REST endpoints and commands implemented; the Kafka order flow (`OrderPlaced`, `OrderCancelled`, `OrderCompleted`) is not connected yet |
| I2 Receive Stock Notifications | Subscription endpoint implemented; `StockLow` / `ProductRestocked` are raised inside the service, but customers are not notified yet |
| I3 Update Stock Levels | Implemented |

## Endpoints

| Method | Path | Access | Result |
|---|---|---|---|
| GET | `/inventory/{productId}` | Public | 200 `InventoryResponse` (`productId`, `quantity`, `reservedQuantity`) |
| PUT | `/inventory/{productId}` | ADMIN | 200 `InventoryResponse` |
| POST | `/inventory/{productId}/reservations` | ADMIN | 201 `StockReservationResponse` |
| DELETE | `/inventory/{productId}/reservations/{reservationId}` | ADMIN | 204 |
| POST | `/inventory/{productId}/notifications` | Customer (the token's user must be the `userId` in the body) | 201 `StockNotificationResponse` |

Errors use the body `{ "error": "<code>", "message": "<explanation>" }`, with the codes of the API error contract (`ValidationFailed` 400, `AuthenticationRequired` 401, `AccessDenied` 403, `ResourceNotFound` 404, `StockUnavailable` 409, `ReservationConflict` 409, `InternalServiceError` 500).

## How it works

- **Event sourcing:** `InventoryItem` is the only event-sourced aggregate. Its events are stored in the table `inventory_event`, and its state is rebuilt by replaying them. The stream id is `inventory-item-<productId>`.
- **CQRS:** `InventoryCommandService` loads the aggregate from the event store and appends events. `InventoryProjection` updates the read model `inventory_read_model` and the stock reservations from those events. `InventoryQueryService` reads the read model only.
- **Derived events:** `StockLow` and `ProductRestocked` are worked out from the change in available stock (`quantity - reservedQuantity`) and published inside the service.

## Configuration

| Setting | Environment variable | Default | Meaning |
|---|---|---|---|
| `security.jwt.secret` | `JWT_SECRET` | none (required) | Shared secret for validating tokens; same value as in Account Service, at least 32 bytes |
| `inventory.low-stock-threshold` | `LOW_STOCK_THRESHOLD` | 10 | `StockLow` is raised when available stock falls below this value |

Run (from the repository root): set `JWT_SECRET`, then `./mvnw spring-boot:run -pl inventory-service`.

## Assumptions

- The first stock update for a product creates its inventory record; Inventory Service does not check that the product exists in Catalog Service.
- A stock update below the reserved quantity returns `ValidationFailed` (400).
- Releasing a reservation that does not exist, or is not `ACTIVE`, returns `ReservationConflict` (409).
- A customer can only subscribe to a product that has an inventory record; subscribing twice returns the existing subscription.
- `StockLow` is raised once when available stock crosses below the threshold, not on every change while it stays below.
- The first stock update from 0 raises `ProductRestocked`.

## Not done yet

Waiting for Architecture Decision 14 (Kafka library and event contracts, to be agreed with Order Service):

- Consuming `OrderPlaced`, `OrderCancelled` and `OrderCompleted`, and publishing `StockReserved`, `StockReservationFailed` and `StockChanged`. The commands these events trigger already exist.
- Notifying subscribed customers about `StockLow` and `ProductRestocked`.
- `CheckStockAvailabilityQuery` (no endpoint uses it yet).
