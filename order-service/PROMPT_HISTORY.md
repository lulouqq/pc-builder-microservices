# Prompt History – order-service

This file records AI-assisted development prompts that relate **only to `order-service`**.
Project-wide prompts (setup, architecture, specifications, cross-service changes) are recorded in the root [`PROMPT_HISTORY.md`](../PROMPT_HISTORY.md).

For every substantial AI-assisted change to this service, append an entry with: date, goal, exact user prompt, short summary of changes, and files created/modified.

---

## Prompt 1

**Date:** 2026-10-14

**Goal:** Rename the working branch to cover inventory and order service, and review whether order-service is implementable and whether it resolves the issues left open in inventory-service. Review only — no code or spec changes.

**Exact user prompt:**

````text
Rename branch to inventory and order service and extend the branch's scope to order service as well. Check order service for implementability. Inform me if order service is implementable and if it addresses the issues in Inventory service.
````

**Branch:** local branch renamed from `feature/inventory-service` to `feature/inventory-order-service`. The remote branch `origin/feature/inventory-service` was not renamed or deleted.

**State at review time:** `order-service/` contains only `README.md` and this file. Catalog (C1, C2, C4, C5), Account (A1–A3), Recommendation (R1–R4) and the REST side of Inventory are implemented. No service has a Kafka dependency or configuration (Architecture Decision 14, Open).

**Well specified:** the ten Order endpoints; `Order` and `OrderItem` fields; the lifecycle `DRAFT → PLACED → PROCESSING → COMPLETED / CANCELLED` with every transition, trigger and Kafka event; `OrderStateViolation` (409); access rules (all Order endpoints are Customer (owner), 404 for an unknown order and 403 for another user's order); the REST calls to Account (saved addresses, forwarded token), Catalog (public) and Recommendation (R3, forwarded token), all of which are implemented on the other side.

**Not defined — decisions needed before the REST side:**

1. Fields of `OrderCreateRequest`, `OrderItemCreateRequest`, `DeliveryAddressRequest`, `PaymentStatusRequest` and `OrderResponse`.
2. Where `unitPrice` comes from (Catalog price when the item is added), which Catalog endpoint is used (`GET /products/{productId}` will publish `ProductViewed` and affect trending; Recommendation uses `GET /products` for that reason), and for R3 whether the recommendation's component price or the current Catalog price is used.
3. Adding a product that is already in the cart: increase the quantity or add a second item. R3 components have no quantity (assumed 1).
4. Checkout preconditions: whether an empty cart can be checked out, whether a delivery address is required, and in which statuses the delivery address can be set.
5. Whether a customer can have several DRAFT orders.
6. Payment: accepted values in `PaymentStatusRequest`, the time limit after which an unpaid `PROCESSING` order is cancelled (FR-06 says "a defined time period") and how it is enforced.
7. When Order Service returns `StockUnavailable` (the contract lists it for Order, but reservation is asynchronous).
8. Error body field name: catalog-service and inventory-service use `error`, account-service and recommendation-service use `code`.

**Not defined — Kafka side (Architecture Decision 14):** library, topic names, payloads of `OrderPlaced`, `OrderCancelled`, `OrderCompleted`, `StockReserved`, `StockReservationFailed`; whether a multi-item order is reserved all-or-nothing.

**Effect on the open inventory-service issues:**

| Inventory issue | Resolved by order-service work? |
|---|---|
| Kafka library and event contracts | Yes — Order publishes three of the five order-flow events and owns the item list, so the contracts are defined here |
| Multi-item reservation, and `StockReserved` per order or per item | Yes — the lifecycle has one `PLACED → PROCESSING / CANCELLED` step per order, which implies one result per order and all-or-nothing reservation |
| Inventory Kafka consumers and publishers not connected | Enabled, but the consumers still have to be written in inventory-service |
| Concurrent writes to one inventory stream surface as 500 | No — and it becomes more likely once several orders reserve the same product through Kafka |
| Notifying subscribed customers (I2) | No |
| Error body mismatch between services | No — Order has to choose a side |
| catalog-service: contract still described as provisional, no JWT validation | No |
| `CheckStockAvailabilityQuery` unused | No |

**Conclusion:** order-service is implementable. The REST side (O1, O2, O4 recording, O5, O6, R3) can be built after decisions 1–8; the Kafka flow (O3, the events of O1/O4/O6) needs Architecture Decision 14.

**Files created/modified:**

- `order-service/PROMPT_HISTORY.md` (this entry only)

---

## Prompt 2

**Date:** 2026-10-14

**Goal:** Apply the eight decisions from the Prompt 1 review and build the REST side of order-service; draft the Kafka contracts for approval (not implemented).

**Exact user prompt:**

````text
Decisions required before REST side:
1. Create the fields of the DTOs as required.
2. Catalog's stored price
3. Increase quantity
4. Empty cart not allowed, and delivery address required for checkout. 
5. Can have 1 draft order
6. Time limit is 10 minutes. Scheduled timeout check in the Order Service, with Kafka events to notify other services. Define accepted values which you think is best.
7. return 202 Accepted initially, then communicate the reservation result asynchronously through Kafka.
8. Update error body field name to match account and recommendation.

For the Kafka side, make a draft and take my approval.
````

**Summary of changes:**

- Module: `order-service` added (Spring Web, Data JPA, Validation, Security, OAuth2 resource server, H2); port 8083, base path `/api`, H2 `orderdb`.
- Domain: `Order` aggregate root with `OrderItem` entities, `OrderStatus` and `PaymentStatus`; every lifecycle transition is a method of `Order` that rejects invalid states with `OrderStateViolationException`. Domain events `OrderPlaced`, `OrderCancelled`, `OrderCompleted` and `PaymentStatusUpdated` are registered by the aggregate and published in-process when it is saved (not Kafka yet).
- Service: `OrderService` (cart, delivery address, checkout, payment, cancel, history, order from a recommendation, reservation results, payment timeout) with the ports `CatalogClient`, `AccountClient` and `RecommendationClient`.
- Infrastructure: `OrderRepository`, REST adapters for the three ports, `PaymentTimeoutScheduler`, `JwtConfig`, `SecurityConfig`.
- Presentation: `OrderController` (the ten endpoints), DTOs, `GlobalExceptionHandler`, `SecurityErrorHandler`; error body `{code, message}`.
- Decisions applied: DTO fields defined (1); unit price is Catalog's current price (2); adding a product again increases its quantity (3); checkout needs items and a delivery address (4); one DRAFT order per customer (5); 10-minute payment limit with a scheduled check, `PaymentStatusRequest` accepts `PAID` or `FAILED` (6); checkout returns 202 Accepted and never `StockUnavailable` (7); error field is `code` (8).
- Kafka: draft of library, topics, event payloads and rules written in `order-service/README.md` for approval. Nothing Kafka-related was implemented.
- Tests: `OrderTests` (15), `OrderApiTests` (25, the three clients mocked), `RestClientTests` (6, local stub HTTP server), context load (1). `./mvnw clean test` passes for all modules (catalog 46, account 68, recommendation 72, inventory 31, order 47).

**Assumptions (not defined in the specs or the prompt):**

- The delivery address can only be selected while the order is `DRAFT`; an unknown product or address returns `ResourceNotFound` (404); an empty cart or missing address at checkout returns `OrderStateViolation` (409).
- Catalog is read with `GET /products` (not `GET /products/{productId}`), as in Recommendation Service.
- Adding a product that is already in the cart also updates its unit price to the current price.
- `POST /orders/from-recommendation/{recommendationId}` adds to the existing cart if there is one; quantity 1 per component, current Catalog price.
- The time limit starts when the order becomes `PROCESSING`; `Order.paymentDueAt` was added to the domain model for it.
- `OrderCancelled` carries a `reason`; it is not published for a cancelled `DRAFT` order or after `StockReservationFailed`.

**Not done / open:**

- Kafka publishing and consuming (Architecture Decision 14, draft awaiting approval).
- A `PLACED` order whose reservation result never arrives stays `PLACED`.
- catalog-service still uses the error field `error` (not changed; outside this branch's scope).

**Files created/modified:**

- `order-service/pom.xml`
- `order-service/src/main/resources/application.yml`
- `order-service/src/main/java/com/csci318/orderservice/OrderServiceApplication.java`
- `order-service/src/main/java/com/csci318/orderservice/domain/*.java`
- `order-service/src/main/java/com/csci318/orderservice/domain/event/*.java`
- `order-service/src/main/java/com/csci318/orderservice/infrastructure/*.java`
- `order-service/src/main/java/com/csci318/orderservice/service/*.java`
- `order-service/src/main/java/com/csci318/orderservice/presentation/*.java`
- `order-service/src/main/java/com/csci318/orderservice/presentation/dto/*.java`
- `order-service/src/test/resources/application-test.yml`
- `order-service/src/test/java/com/csci318/orderservice/*.java`
- `order-service/README.md`, `order-service/PROMPT_HISTORY.md`
- Project-wide and inventory-service files: see root `PROMPT_HISTORY.md` Prompt 26
