# Prompt History – catalog-service

This file records AI-assisted development prompts that relate **only to `catalog-service`**.
Project-wide prompts (setup, architecture, specifications, cross-service changes) are recorded in the root [`PROMPT_HISTORY.md`](../PROMPT_HISTORY.md).

For every substantial AI-assisted change to this service, append an entry with: date, goal, exact user prompt, short summary of changes, and files created/modified.

---

## Prompt 1

**Date:** 2026-10-10

**Goal:** Implement user story C5 (manage products and categories) in `catalog-service`.

**Exact user prompt:**

````text
Implement C5 to make catalog service ready for starting implementation.
````

**Summary of changes:**

- Domain: `Product` and `Category` aggregate roots, `Specification` value object (embedded in `Product`), and the internal domain events `ProductCreated`, `ProductUpdated` and `ProductRemoved`.
- Infrastructure: `ProductRepository` and `CategoryRepository` (Spring Data JPA, H2).
- Service: `ProductService` and `CategoryService`; product events are published in-process with Spring's `ApplicationEventPublisher` (not Kafka, per Architecture Decision 9).
- Presentation: the six C5 endpoints (`POST /products`, `PUT`/`DELETE /products/{productId}`, `POST /categories`, `PUT`/`DELETE /categories/{categoryId}`), request/response DTOs, and a `GlobalExceptionHandler` returning `ValidationFailed` (400) and `ResourceNotFound` (404).
- Added `spring-boot-starter-validation` for request validation.
- Tests: `CatalogManagementTests` (8 tests) covering create/update/remove, validation, not found, events and removing a category that still has products. `./mvnw clean test` passes (9 tests).
- Assumptions (not defined in the specs): error body is `{error, message}`; a product with an unknown `categoryId` is rejected with `ValidationFailed` (400); price must not be negative; removing a category that still has products is rejected with 409 and the code `CategoryInUse`, which is not in the API error contract.
- Not implemented: admin authorisation for the C5 endpoints (C5 rule 1, NFR-04), because no mechanism is specified.

**Files created/modified:**

- `catalog-service/pom.xml`
- `catalog-service/README.md`
- `catalog-service/PROMPT_HISTORY.md`
- `catalog-service/src/main/java/com/csci318/catalogservice/domain/{Product,Category,Specification}.java`
- `catalog-service/src/main/java/com/csci318/catalogservice/domain/event/{ProductCreated,ProductUpdated,ProductRemoved}.java`
- `catalog-service/src/main/java/com/csci318/catalogservice/infrastructure/{ProductRepository,CategoryRepository}.java`
- `catalog-service/src/main/java/com/csci318/catalogservice/service/{ProductService,CategoryService,ResourceNotFoundException,ValidationFailedException,CategoryInUseException}.java`
- `catalog-service/src/main/java/com/csci318/catalogservice/presentation/{ProductController,CategoryController,GlobalExceptionHandler}.java`
- `catalog-service/src/main/java/com/csci318/catalogservice/presentation/dto/*.java`
- `catalog-service/src/test/java/com/csci318/catalogservice/CatalogManagementTests.java`

---

## Prompt 2

**Date:** 2026-10-10

**Goal:** Fix the issues left unresolved after C5 and implement user story C1 (browse and search products).

**Exact user prompt:**

````text
Fix the unresolved issues and Implement C1.
````

**Summary of changes:**

- C1: `GET /products` with the optional filters `categoryId`, `brand` and `specName` + `specValue` (combined with AND, results ordered by name), and `GET /categories` (ordered by name). `ProductRepository.search` implements the filters in one JPQL query.
- Unexpected errors: `GlobalExceptionHandler` now extends `ResponseEntityExceptionHandler` and returns `InternalServiceError` (500) for unexpected exceptions; standard Spring MVC errors use the contract's codes (`ValidationFailed`, `ResourceNotFound`).
- `CategoryInUse` (409): kept in code and added to the API error contract in `specs/api-endpoints.md`.
- Admin authorisation: the user chose to leave it open; recorded as open Architecture Decision 11 in `specs/architecture.md`. The C5 endpoints remain unprotected.
- Root `README.md`: catalog-service status changed from "skeleton" to "C1, C5 implemented".
- Tests: new `ProductBrowsingTests` (7 tests); `CatalogManagementTests` now also checks the `CategoryInUse` code and the error codes for malformed bodies and unknown paths. `./mvnw clean test` passes (17 tests).
- Assumptions (not defined in the specs): brand and specification matching ignores case; `specName` or `specValue` on its own is rejected with `ValidationFailed` (400); results are ordered by name.

**Files created/modified:**

- `catalog-service/src/main/java/com/csci318/catalogservice/infrastructure/ProductRepository.java`
- `catalog-service/src/main/java/com/csci318/catalogservice/service/{ProductService,CategoryService}.java`
- `catalog-service/src/main/java/com/csci318/catalogservice/presentation/{ProductController,CategoryController,GlobalExceptionHandler}.java`
- `catalog-service/src/test/java/com/csci318/catalogservice/{ProductBrowsingTests,CatalogManagementTests}.java`
- `catalog-service/README.md`, `catalog-service/PROMPT_HISTORY.md`
- `specs/api-endpoints.md`, `specs/architecture.md`, `README.md`, `PROMPT_HISTORY.md` (project-wide files; see root Prompt 14)

---

## Prompt 3

**Date:** 2026-10-10

**Goal:** Commit the C5 and C1 work, then implement user story C4 (submit and view product reviews) with a 1–5 star rating.

**Exact user prompt:**

````text
Leave admin authorisation for after account microservice is implemented. Commit C5 and C! work. For C4, the rating range should be 1-5 stars. Implement C4.
````

**Summary of changes:**

- Committed the C5 and C1 work as `126cb2c` ("feat(catalog): implement C5 and C1"); not pushed. The C4 work below is not committed.
- Domain: `Review` aggregate root (id, productId, userId, rating 1–5, optional comment, createdAt) and the internal domain event `ProductReviewed`.
- Infrastructure: `ReviewRepository`.
- Service: `ReviewService` submits and lists reviews, publishes `ProductReviewed` in-process, and removes a product's reviews when `ProductRemoved` is published.
- Presentation: `GET` and `POST /products/{productId}/reviews` in `ReviewController`, with `ReviewCreateRequest` and `ReviewResponse`.
- Specs: `Review.rating` is now "Required, 1–5" in `specs/domain-model.md` and `specs/api-endpoints.md`; Architecture Decision 11 (admin authorisation) is marked as deferred until Account Service is implemented.
- Tests: new `ProductReviewTests` (8 tests). `./mvnw clean test` passes (25 tests).
- Assumptions (not defined in the specs): reviews are listed newest first; an unknown product returns `ResourceNotFound` (404) for both endpoints; `userId` is not checked against Account Service (the architecture has no Catalog → Account call); a user may review the same product more than once; comments are limited to 2000 characters; a removed product's reviews are removed with it.

**Files created/modified:**

- `catalog-service/src/main/java/com/csci318/catalogservice/domain/Review.java`
- `catalog-service/src/main/java/com/csci318/catalogservice/domain/event/ProductReviewed.java`
- `catalog-service/src/main/java/com/csci318/catalogservice/infrastructure/ReviewRepository.java`
- `catalog-service/src/main/java/com/csci318/catalogservice/service/ReviewService.java`
- `catalog-service/src/main/java/com/csci318/catalogservice/presentation/ReviewController.java`
- `catalog-service/src/main/java/com/csci318/catalogservice/presentation/dto/{ReviewCreateRequest,ReviewResponse}.java`
- `catalog-service/src/test/java/com/csci318/catalogservice/ProductReviewTests.java`
- `catalog-service/README.md`, `catalog-service/PROMPT_HISTORY.md`
- `specs/domain-model.md`, `specs/api-endpoints.md`, `specs/architecture.md`, `README.md`, `PROMPT_HISTORY.md` (project-wide files; see root Prompt 15)

---

## Prompt 4

**Date:** 2026-10-10

**Goal:** Commit C4, then progress C2 and C3 as far as possible without inventing API contracts, Kafka event schemas or business rules.

**Exact user prompt:**

````text
Commit C4 first, preserving the existing implementation.

For C2 and C3, do not block all progress just because their external dependencies are not ready. However, do not invent API contracts, Kafka event schemas, or business rules.

For C2:
Inspect the existing requirements and determine what Catalog needs from Inventory.

Identify the proposed GET /inventory/{productId} response contract.

Treat quantity and reservedQuantity as provisional until confirmed against the Inventory implementation or agreed contract.

Implement Catalog against an interface/client that can be mocked in tests.

Handle unavailable Inventory data distinctly from confirmed zero stock.

Document any remaining integration assumptions.

For C3:
Identify the missing decisions required to define trending products.

Propose a clear event schema and trending calculation for team approval.

Implement independently testable calculation logic using sample events where possible.

Do not add Kafka infrastructure or invent producer behaviour until the existing project configuration and team requirements have been checked.

Before modifying files, report the decisions that require team confirmation and the implementation steps that can proceed independently. Keep the changes focused on Catalog and do not modify other services without approval.
````

**Summary of changes:**

- Committed C4 as `c1c779c` ("feat(catalog): implement C4 product reviews"); not pushed. The work below is not committed.
- Checked before changing files: inventory-service has no code, and no Kafka dependency or configuration exists in the project. Reported the decisions needing team confirmation and the independent steps before modifying files.
- C2: `GET /products/{productId}` returns the product with `stockStatus`. New `StockStatus` enum (`IN_STOCK`, `OUT_OF_STOCK`, `UNKNOWN`), `InventoryClient` interface (service layer) and `RestInventoryClient` (infrastructure), which holds the provisional `InventoryResponse` contract (`quantity`, `reservedQuantity`). Unavailable or unusable Inventory data gives `UNKNOWN`; confirmed zero available stock gives `OUT_OF_STOCK`. New settings `inventory-service.base-url` and `inventory-service.timeout`.
- C3: `TrendingProductsCalculator` (domain service) and a provisional `ProductViewed` record; the window and list size are parameters. No Kafka, no producer, no consumer and no `GET /products/trending` endpoint.
- `ProductViewed` is not published from `GET /products/{productId}` yet, although `specs/api-endpoints.md` says C2 publishes it; this waits for the Kafka decisions.
- `catalog-service/README.md`: status table, C2 integration assumptions, and the C3 decisions, proposed event schema and proposed calculation for team approval.
- No specs, other services or root files were changed.
- Tests: new `ProductDetailsTests` (6, `InventoryClient` mocked), `RestInventoryClientTests` (8, local stub HTTP server) and `TrendingProductsCalculatorTests` (7, sample events). `./mvnw clean test` passes (46 tests).

**Files created/modified:**

- `catalog-service/src/main/java/com/csci318/catalogservice/domain/{StockStatus,TrendingProductsCalculator}.java`
- `catalog-service/src/main/java/com/csci318/catalogservice/domain/event/ProductViewed.java`
- `catalog-service/src/main/java/com/csci318/catalogservice/service/{InventoryClient,ProductDetails,ProductService}.java`
- `catalog-service/src/main/java/com/csci318/catalogservice/infrastructure/RestInventoryClient.java`
- `catalog-service/src/main/java/com/csci318/catalogservice/presentation/ProductController.java`
- `catalog-service/src/main/java/com/csci318/catalogservice/presentation/dto/ProductResponse.java`
- `catalog-service/src/main/resources/application.yml`
- `catalog-service/src/test/java/com/csci318/catalogservice/{ProductDetailsTests,RestInventoryClientTests,TrendingProductsCalculatorTests}.java`
- `catalog-service/README.md`, `catalog-service/PROMPT_HISTORY.md`
