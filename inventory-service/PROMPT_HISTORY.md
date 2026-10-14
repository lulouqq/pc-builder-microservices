# Prompt History – inventory-service

This file records AI-assisted development prompts that relate **only to `inventory-service`**.
Project-wide prompts (setup, architecture, specifications, cross-service changes) are recorded in the root [`PROMPT_HISTORY.md`](../PROMPT_HISTORY.md).

For every substantial AI-assisted change to this service, append an entry with: date, goal, exact user prompt, short summary of changes, and files created/modified.

---

## Prompt 1

**Date:** 2026-10-14

**Goal:** Apply the decisions on the five inventory spec gaps and build the REST side of inventory-service (I3, I1 REST, I2 subscription) with event sourcing, CQRS and JWT validation; determine the Kafka library and whether the remaining Kafka gaps are better resolved with Order Service.

**Exact user prompt:**

````text
1. Recommendation will expect GET /inventory/{productId} to return productId, quantity, reservedQuantity as the InventoryResponse contract. For the rest, define them as necessary.

2. Create an inventory record automatically when the first stock update arrives for a product without an existing record. Check the requirements and current implementation to see whether this is appropriate. If nothing specifies another initialization mechanism, proceed with this approach and document the assumption.

3. Set absolute quantity for stock. Reason is a request field if the system needs to record why stock changed; otherwise, don't require it. Reject updates where the new quantity would be lower than the reserved quantity.

4. Use a consistent error response body of { "error": "ERROR_CODE", "message": "Human-readable explanation" } across the inventory service. Check the existing services and API conventions first, and align with the established format if one already exists. Keep HTTP status codes appropriate to each error.

5. Use a configurable low-stock threshold, with a default value of 10 units unless the existing requirements or configuration specify otherwise. Publish StockLow when the available stock (quantity - reservedQuantity) falls below the configured threshold. Document the assumption and avoid hardcoding the threshold in business logic.

For Kafka Side gaps:
Apache Kafka is a project requirement. Use Apache Kafka for asynchronous communication between microservices. Check the existing dependencies and configuration to determine the appropriate Kafka integration library. 
For rest of the gaps, check if implementing Order service will better to resolve those gaps. I am building order service as well.
````

**Checks made before implementing:**

- Initialisation (decision 2): the specs define no other mechanism. `InventoryInitialized` is "created internally", Catalog's `ProductCreated` is not published to Kafka (Architecture Decision 9), and there is no Inventory → Catalog call. Creating the record on the first stock update is appropriate; the endpoint is ADMIN-only.
- Error body (decision 4): catalog-service uses `{error, message}` and account-service uses `{code, message}`, so there is no single established format. Inventory uses `{error, message}` with the error codes of the API error contract (e.g. `ValidationFailed`), matching catalog-service.
- Threshold (decision 5): no requirement or configuration specifies a value; default 10 is used.
- Kafka: no service has a Kafka dependency or configuration. Recommended library: Spring for Apache Kafka (`spring-kafka`), whose version is managed by the existing Spring Boot parent POM. Not added yet (root `PROMPT_HISTORY.md`, Prompt 25; Architecture Decision 14).

**Summary of changes:**

- Module: `inventory-service` added (Spring Web, Data JPA, Validation, Security, OAuth2 resource server, H2); port 8082, base path `/api`, H2 `inventorydb`.
- Domain: event-sourced `InventoryItem` aggregate (`initialize`, `updateStock`, `reserveStock`, `releaseReservation`, `confirmReservation`, `rehydrate`); events `InventoryInitialized`, `StockLevelUpdated`, `StockReserved`, `StockReservationReleased`, `StockReservationConfirmed`; derived events `StockLow`, `ProductRestocked`; `StockNotificationSubscribed`; `StockAlertPolicy` domain service; `StockReservation` and `StockNotificationSubscription` entities; domain exceptions.
- Infrastructure: event store (`StoredEvent`, table `inventory_event`, unique stream version; `InventoryEventStore`), read model (`InventoryReadModel`, table `inventory_read_model`), `InventoryProjection`, repositories, `JwtConfig` and `SecurityConfig` (same JWT design as account-service).
- Service (CQRS): `InventoryCommandService` with the five commands of the command catalog, and `InventoryQueryService` with `GetInventoryByProductQuery`.
- Presentation: `InventoryController` (the five endpoints), DTOs, `GlobalExceptionHandler`, `SecurityErrorHandler`.
- Configuration: `security.jwt.secret` from `JWT_SECRET` (no default); `inventory.low-stock-threshold` from `LOW_STOCK_THRESHOLD` (default 10).
- Tests: `InventoryItemTests` (9), `StockAlertPolicyTests` (5), `InventoryApiTests` (16), context load (1). `./mvnw clean test -pl inventory-service` passes (31 tests).

**Assumptions (not defined in the specs or the prompt):**

- The stream id is `inventory-item-<productId>`.
- A stock update below the reserved quantity, or a negative one, returns `ValidationFailed` (400).
- Releasing an unknown or non-`ACTIVE` reservation returns `ReservationConflict` (409); reservation endpoints for a product without an inventory record return 404.
- Subscribing needs an existing inventory record (404 otherwise); subscribing twice returns the existing subscription.
- `StockLow` is raised once when available stock crosses below the threshold; the first stock update from 0 raises `ProductRestocked`.
- Payloads of `StockLow`, `ProductRestocked` and `StockNotificationSubscribed` are not in the specs; they are in-process events only.
- Product existence is not checked against Catalog Service.

**Not done / open:**

- Kafka: consuming `OrderPlaced`, `OrderCancelled`, `OrderCompleted`; publishing `StockReserved`, `StockReservationFailed`, `StockChanged` (Architecture Decision 14, Open).
- Notifying subscribed customers (I2); `CheckStockAvailabilityQuery` (no endpoint uses it).
- Concurrent writes to one stream are rejected by the unique version constraint but surface as 500.
- catalog-service still describes the `InventoryResponse` contract as provisional and has no JWT validation; account-service's error body uses `code` instead of `error`. Not changed (other services).

**Files created/modified:**

- `inventory-service/pom.xml`
- `inventory-service/src/main/resources/application.yml`
- `inventory-service/src/main/java/com/csci318/inventoryservice/InventoryServiceApplication.java`
- `inventory-service/src/main/java/com/csci318/inventoryservice/domain/*.java`
- `inventory-service/src/main/java/com/csci318/inventoryservice/domain/event/*.java`
- `inventory-service/src/main/java/com/csci318/inventoryservice/infrastructure/*.java`
- `inventory-service/src/main/java/com/csci318/inventoryservice/service/*.java`
- `inventory-service/src/main/java/com/csci318/inventoryservice/presentation/*.java`
- `inventory-service/src/main/java/com/csci318/inventoryservice/presentation/dto/*.java`
- `inventory-service/src/test/resources/application-test.yml`
- `inventory-service/src/test/java/com/csci318/inventoryservice/*.java`
- `inventory-service/README.md`, `inventory-service/PROMPT_HISTORY.md`
- `pom.xml`, `README.md`, `specs/api-endpoints.md`, `specs/event-sourcing.md`, `specs/architecture.md`, `PROMPT_HISTORY.md` (project-wide files; see root Prompt 25)

---

## Prompt 2

**Date:** 2026-10-14

**Goal:** Analyse I2 (stock notifications) and propose the simplest complete design for delivering notifications to subscribed customers. Analysis only — no code or spec changes.

**Exact user prompt:**

````text
Inspect the current implementation of I2 in Inventory Service, FR-07, related specs/domain model, and the current inventory-stock-events Kafka flow.
Come up with the simplest complete option for delivering notifications about products the user subscribed to, without adding new libraries or external services. A good option would be to save notifications in H2 and provide a REST endpoint for the client to retrieve them.
At first, don't change anything in the code. Only provide:
how I2 works now and what is still missing;
a minimal solution design;
which new or changed REST endpoints and DTOs are needed;
which changes are needed in the domain model, specs, and architecture;
how the Kafka flow will work from StockChanged to the user notification;
which decisions need to be made before implementation;
which tests need to be added.
````

**Summary of changes:**

- No code or spec changes. Design proposal given in the reply: Inventory Service consumes its own `inventory-stock-events`, stores one notification per subscriber in H2 for `STOCK_LOW` / `PRODUCT_RESTOCKED`, and serves them with an owner-only `GET /users/{userId}/...` endpoint; in-process fallback when Kafka is disabled. Open decisions and the test plan are listed in the reply.

**Files created/modified:**

- `inventory-service/PROMPT_HISTORY.md`

---

## Prompt 3

**Date:** 2026-10-14

**Goal:** Implement I2 stock alerts (stored in H2, read with `GET /users/{userId}/stock-alerts`) after the approved decisions were added to the specs.

This prompt also changed shared specs, so the full entry (exact prompt, summary, files) is recorded as **Prompt 32** in the root [`PROMPT_HISTORY.md`](../PROMPT_HISTORY.md).
