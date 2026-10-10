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
