# order-service

Responsibilities: cart, checkout, orders, order items, order and payment status. The cart is an Order in `DRAFT` status (no separate Cart entity) — see `specs/domain-model.md`.

Port: 8083

## Status

| Story | Status |
|---|---|
| O1 Add Products to Cart and Checkout | Implemented; checkout publishes `OrderPlaced` to Kafka |
| O2 Select Delivery Address | Implemented |
| O3 Update Order Fulfilment Status | Implemented: `StockReserved` / `StockReservationFailed` are consumed from Kafka |
| O4 Process Payment Status | Implemented, including the 10-minute payment time limit; publishes `OrderCompleted` / `OrderCancelled` to Kafka |
| O5 View Order History and Track Orders | Implemented |
| O6 Cancel Order | Implemented; publishes `OrderCancelled` to Kafka |
| R3 Add Recommended Build to Cart | Implemented |

## Endpoints

Every endpoint needs the customer's token, and the token's user must own the order (403 otherwise; 404 for an unknown order).

| Method | Path | Result |
|---|---|---|
| POST | `/orders` | 201, the customer's cart (a `DRAFT` order) |
| POST | `/orders/{orderId}/items` | 201 |
| DELETE | `/orders/{orderId}/items/{orderItemId}` | 204 |
| PUT | `/orders/{orderId}/delivery-address` | 200 |
| POST | `/orders/{orderId}/checkout` | 202, order is `PLACED`; the reservation result arrives asynchronously |
| PUT | `/orders/{orderId}/payment` | 200 |
| GET | `/orders/{orderId}` | 200 |
| GET | `/users/{userId}/orders` | 200, newest first |
| POST | `/orders/{orderId}/cancel` | 200 |
| POST | `/orders/from-recommendation/{recommendationId}` | 201 |

Errors use the body `{ "code": "<code>", "message": "<explanation>" }`, with the codes of the API error contract (`ValidationFailed` 400, `AuthenticationRequired` 401, `AccessDenied` 403, `ResourceNotFound` 404, `OrderStateViolation` 409, `InternalServiceError` 500, `ServiceUnavailable` 503).

## Calls to other services

| Service | Call | Used for |
|---|---|---|
| Catalog | `GET /products` (public) | The product's current price when an item is added |
| Account | `GET /users/{userId}/addresses` (customer's token forwarded) | Checking that the delivery address is one of the customer's saved addresses |
| Recommendation | `GET /recommendations/{recommendationId}` (customer's token forwarded) | The components of a recommended build |

## Configuration

| Setting | Environment variable | Default | Meaning |
|---|---|---|---|
| `security.jwt.secret` | `JWT_SECRET` | none (required) | Shared secret for validating tokens; same value as in Account Service, at least 32 bytes |
| `order.payment-time-limit` | `PAYMENT_TIME_LIMIT` | `10m` | Time allowed for payment after the order becomes `PROCESSING` |
| `order.payment-timeout-check-interval` | `PAYMENT_TIMEOUT_CHECK_INTERVAL` | `PT30S` | How often overdue orders are looked for (ISO-8601 duration) |
| `spring.kafka.bootstrap-servers` | `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker |
| `messaging.kafka.enabled` | `KAFKA_ENABLED` | `true` | `false` runs the service without a broker; events are then only raised inside the service |

Run (from the repository root): start the broker with `docker compose up -d`, set `JWT_SECRET`, then `./mvnw spring-boot:run -pl order-service`.

## Assumptions

- The delivery address can only be selected while the order is `DRAFT`.
- Checking out an empty cart, or one without a delivery address, returns `OrderStateViolation` (409).
- A product or saved address that does not exist returns `ResourceNotFound` (404).
- Adding a product that is already in the cart also updates its unit price to the current Catalog price.
- `POST /orders/from-recommendation/{recommendationId}` adds the components to the existing cart when the customer already has one; each component has quantity 1 and the current Catalog price, not the price stored in the recommendation.
- The payment time limit starts when the order becomes `PROCESSING`. `Order.paymentDueAt` was added to the domain model for it.
- A reservation result for an order that is no longer `PLACED` is ignored.
- A `PLACED` order whose reservation result never arrives stays `PLACED`; only the payment has a time limit.

## Kafka messaging

Contracts: `specs/architecture.md`, "Kafka Event Contracts" (Architecture Decision 14).

| Direction | Topic | Events | Class |
|---|---|---|---|
| Publishes | `order-events` (key: order id) | `OrderPlaced`, `OrderCancelled`, `OrderCompleted` | `OrderEventKafkaPublisher` |
| Consumes | `inventory-reservation-events` (group `order-service`) | `StockReserved`, `StockReservationFailed` | `ReservationEventKafkaListener` |

- An event is sent after the order's database transaction has committed. If the send fails (broker down), the error is logged and the order keeps its new status; the event is not sent again.
- `OrderCancelled` is published only for an order that was `PLACED` or `PROCESSING`.
- A reservation result for an order that is not `PLACED`, an unknown order, or a message that is not valid JSON is skipped.
- `PaymentStatusUpdated` stays inside the service.
- Tests: `OrderKafkaMessagingTests` checks the JSON without a broker; `OrderKafkaIntegrationTests` runs the flow through an embedded broker.
