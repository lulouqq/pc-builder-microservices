# inventory-service

Responsibilities: stock, inventory items, stock reservations, stock notification subscriptions and stock alerts.

Port: 8082

## Status

| Story | Status |
|---|---|
| I1 Reserve Stock During Checkout | Implemented: the Kafka order flow (`OrderPlaced`, `OrderCancelled`, `OrderCompleted`) and the REST endpoints |
| I2 Receive Stock Notifications | Implemented: subscription, stock alerts created from `StockChanged`, and `GET /users/{userId}/stock-alerts` (polling) |
| I3 Update Stock Levels | Implemented |

## Endpoints

| Method | Path | Access | Result |
|---|---|---|---|
| GET | `/inventory/{productId}` | Public | 200 `InventoryResponse` (`productId`, `quantity`, `reservedQuantity`) |
| PUT | `/inventory/{productId}` | ADMIN | 200 `InventoryResponse` |
| POST | `/inventory/{productId}/reservations` | ADMIN | 201 `StockReservationResponse` |
| DELETE | `/inventory/{productId}/reservations/{reservationId}` | ADMIN | 204 |
| POST | `/inventory/{productId}/notifications` | Customer (the token's user must be the `userId` in the body) | 201 `StockNotificationResponse` |
| GET | `/users/{userId}/stock-alerts` | Customer (the token's user must be `{userId}`; ADMIN does not bypass it) | 200 array of `StockAlertResponse` (`id`, `userId`, `productId`, `type`, `availableQuantity`, `message`, `occurredAt`), newest first; empty when there are none |

Errors use the body `{ "code": "<code>", "message": "<explanation>" }`, with the codes of the API error contract (`ValidationFailed` 400, `AuthenticationRequired` 401, `AccessDenied` 403, `ResourceNotFound` 404, `StockUnavailable` 409, `ReservationConflict` 409, `InternalServiceError` 500).

## How it works

- **Event sourcing:** `InventoryItem` is the only event-sourced aggregate. Its events are stored in the table `inventory_event`, and its state is rebuilt by replaying them. The stream id is `inventory-item-<productId>`.
- **CQRS:** `InventoryCommandService` loads the aggregate from the event store and appends events. `InventoryProjection` updates the read model `inventory_read_model` and the stock reservations from those events. `InventoryQueryService` reads the read model only.
- **Derived events:** `StockLow` and `ProductRestocked` are worked out from the change in available stock (`quantity - reservedQuantity`) and published inside the service.
- **Stock alerts (I2, Architecture Decision 17):** `StockLow` / `ProductRestocked` are published as `StockChanged`; `StockChangedKafkaListener` consumes them again and `StockAlertService` stores one `StockAlert` (table `stock_alert`) for each subscriber of the product. The customer polls `GET /users/{userId}/stock-alerts`; there is no push delivery. "Real time" means the alert is there within a few seconds of the stock change. With Kafka disabled, `InProcessStockAlertRecorder` creates the alerts directly after the stock change commits.

## Configuration

| Setting | Environment variable | Default | Meaning |
|---|---|---|---|
| `security.jwt.secret` | `JWT_SECRET` | none (required) | Shared secret for validating tokens; same value as in Account Service, at least 32 bytes |
| `inventory.low-stock-threshold` | `LOW_STOCK_THRESHOLD` | 10 | `StockLow` is raised when available stock falls below this value |
| `spring.kafka.bootstrap-servers` | `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker |
| `messaging.kafka.enabled` | `KAFKA_ENABLED` | `true` | `false` runs the service without a broker; events are then only raised inside the service |

Run (from the repository root): start the broker with `docker compose up -d`, set `JWT_SECRET`, then `./mvnw spring-boot:run -pl inventory-service`.

## Kafka messaging

Contracts: `specs/architecture.md`, "Kafka Event Contracts" (Architecture Decision 14).

| Direction | Topic | Events | Class |
|---|---|---|---|
| Consumes | `order-events` (group `inventory-service`) | `OrderPlaced`, `OrderCancelled`, `OrderCompleted` | `OrderEventKafkaListener` → `OrderStockService` |
| Publishes | `inventory-reservation-events` (key: order id) | `StockReserved`, `StockReservationFailed` | `InventoryKafkaPublisher` |
| Publishes | `inventory-stock-events` (key: product id) | `StockChanged` | `InventoryKafkaPublisher` |
| Consumes | `inventory-stock-events` (group `inventory-stock-alerts`) | `StockChanged` (`STOCK_LOW`, `PRODUCT_RESTOCKED`) | `StockChangedKafkaListener` → `StockAlertService` |

- **All-or-nothing:** `OrderStockService` first checks that every item of the order can be reserved. If so it issues one `ReserveStockCommand` per item and one `StockReserved` is published for the order. If not, nothing is reserved, no event is stored, and `StockReservationFailed` lists the items that were not available.
- `OrderCancelled` releases and `OrderCompleted` confirms every `ACTIVE` reservation of the order.
- An `OrderPlaced` for an order that already has reservations is skipped.
- An event is sent after the database transaction has committed; a failed send is logged.
- A message that fails, for example because two orders changed the same product at the same moment, is retried by the listener container and then skipped.
- A `StockChanged` creates at most one alert per customer: `stock_alert` has a unique (`source_event_id`, `user_id`), and a repeated message is skipped. `STOCK_LEVEL_UPDATED` creates no alert.
- Tests: `OrderStockFlowTests` drives the flow with JSON messages without a broker; `StockChangedKafkaListenerTests` checks how `StockChanged` is read; `StockAlertApiTests` covers the alerts with Kafka disabled; `InventoryKafkaIntegrationTests` runs the order flow and the stock alerts through an embedded broker.

## Assumptions

- The first stock update for a product creates its inventory record; Inventory Service does not check that the product exists in Catalog Service.
- A stock update below the reserved quantity returns `ValidationFailed` (400).
- Releasing a reservation that does not exist, or is not `ACTIVE`, returns `ReservationConflict` (409).
- A customer can only subscribe to a product that has an inventory record; subscribing twice returns the existing subscription.
- `StockLow` is raised once when available stock crosses below the threshold, not on every change while it stays below.
- The first stock update from 0 raises `ProductRestocked`.
- Subscriptions stay active after an alert. A customer who subscribes after a stock change but before its `StockChanged` is processed may still get that alert.
- An alert names the product by `productId` only (no call to Catalog Service). Over Kafka, `availableQuantity` is `quantity - reservedQuantity` from `StockChanged`, read when the message was published.
- `GET /users/{userId}/stock-alerts` returns an empty list for a user without alerts; Inventory Service does not know which users exist.

## Not done yet

- Stock alerts: unsubscribing, read/unread state, pagination and retention (alerts are kept), and push delivery are out of scope (Architecture Decision 17).
- `CheckStockAvailabilityQuery` (no endpoint uses it yet).
- Over REST, two simultaneous writes to one product's stock are rejected by the database but returned as a 500.
