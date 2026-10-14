# Prompt History

This file records the history of AI-assisted development on this project.
It is used for project-wide work (setup, architecture, specifications, cross-service changes). Prompts that relate only to one microservice are recorded in that service's own `PROMPT_HISTORY.md` (e.g. `catalog-service/PROMPT_HISTORY.md`).
For every substantial AI-assisted change, append an entry with: date, goal, exact user prompt, short summary of changes, and files created/modified.

---

## Prompt 1

**Date:** 2026-09-19

**Goal:** Create the initial repository skeleton (based on the reference repository structure) and set up `catalog-service` as a minimal Spring Boot project that builds and starts.

**Exact user prompt:**

````text
We are starting implementation of a university group project for CSCI318 Software Engineering Practices & Principles.

Use this repository/branch as the primary implementation reference:
https://github.com/gxshub/book-and-library-management-system-v1/tree/iter2-impl

Important:
- Use it as a structural and implementation reference only.
- Follow similar repository layout, Spring Boot conventions, package/layer organization, naming style, and implementation patterns where applicable.
- Do NOT blindly copy business logic from the library/book domain.
- Adapt the structure to our own PC-builder domain.

Our project consists of 5 microservices:

1. account-service
   - users
   - profiles
   - addresses
   - preferences

2. catalog-service
   - products
   - categories
   - specifications
   - prices
   - product reviews

3. inventory-service
   - stock
   - inventory items
   - stock reservations

4. order-service
   - cart
   - checkout
   - orders
   - order items
   - order/payment status

5. recommendation-service
   - AI-generated PC builds
   - compatibility checking
   - previous build requests/recommendations

Technology stack:
- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- H2 for local development/testing
- Kafka / Spring Cloud Stream later
- LangChain4j later for the recommendation service

Architecture:
Each microservice should follow the same layered structure used in our design:

- presentation
- service
- domain
- infrastructure

DDD concepts will include where appropriate:
- Entities
- Value Objects
- Aggregate Roots
- Domain Events
- Domain Services

For now, DO NOT:
- implement Kafka
- implement LangChain4j
- implement external LLM integration
- implement the whole project
- add unnecessary complexity
- introduce infrastructure or libraries that are not currently needed

FIRST TASK ONLY:

1. Inspect the reference repository and understand its implementation structure.
2. Create the initial skeleton for our repository based on the same general approach.
3. Create the 5 service directories:
   - account-service
   - catalog-service
   - inventory-service
   - order-service
   - recommendation-service
4. Start implementation setup with catalog-service only.
5. Set up catalog-service as a minimal Spring Boot project using:
   - Java 21
   - Spring Web
   - Spring Data JPA
   - H2
6. Create only the base package/layer structure for catalog-service:
   - presentation
   - service
   - domain
   - infrastructure
7. Do not implement business features yet unless something minimal is required for the project to compile/start.
8. Make sure catalog-service can build and start successfully.

Prompt history requirement:

Create a file in the repository root called:

PROMPT_HISTORY.md

Use it to preserve the history of AI-assisted development.

For every substantial AI-assisted change, append:
- Date
- Goal
- Exact user prompt
- Short summary of changes made
- Files created/modified

Add this current prompt as Prompt 1.

Very important workflow rules:

- Do NOT commit or push anything.
- Leave all changes uncommitted.
- Do NOT add Co-Authored-By or any AI-related Git trailers.
- Stop after completing this first setup task.
- Before making large assumptions, prefer following the reference repository.
- If the reference implementation does something differently from your usual preferred approach, explain the difference before deviating from it.
- Keep the implementation simple and aligned with what would be expected in a university lab/project.

At the end, provide a short report with:
1. what you inspected in the reference repository
2. what you created
3. repository structure
4. whether catalog-service builds/runs successfully
5. any decisions or assumptions made
6. what the logical next step should be

Do not continue into the next development task until I explicitly ask.
````

**Summary of changes:**

- Inspected the reference repository (`iter2-impl` branch): Maven multi-module root POM with `spring-boot-starter-parent` 3.3.2, one module per microservice, package `com.csci318.<service>`, `application.yml` with H2 in-memory DB and `/api/v1` context path, Maven wrapper at the root.
- Created a Maven multi-module root POM (`pc-builder-microservices`) following the same approach; only `catalog-service` is registered as a module for now.
- Copied the Maven wrapper (`mvnw`, `mvnw.cmd`, `.mvn/`) and `.gitignore` from the reference repository.
- Created the five service directories; the four not-yet-implemented services contain a placeholder `README.md` only.
- Set up `catalog-service` as a minimal Spring Boot app (Spring Web, Spring Data JPA, H2) with the four layer packages `presentation`, `service`, `domain`, `infrastructure` (each with a `package-info.java` so they are kept in Git), `application.yml` (port 8082, context path `/api/v1`, H2 `catalogdb`), and a context-load test.
- Verified: `./mvnw clean test` passes and `./mvnw spring-boot:run -pl catalog-service` starts on port 8082.

**Files created/modified:**

- `pom.xml`
- `README.md`
- `PROMPT_HISTORY.md`
- `.gitignore`
- `mvnw`, `mvnw.cmd`, `.mvn/wrapper/*`
- `catalog-service/pom.xml`
- `catalog-service/src/main/java/com/csci318/catalogservice/CatalogServiceApplication.java`
- `catalog-service/src/main/java/com/csci318/catalogservice/{presentation,service,domain,infrastructure}/package-info.java`
- `catalog-service/src/main/resources/application.yml`
- `catalog-service/src/test/java/com/csci318/catalogservice/CatalogServiceApplicationTests.java`
- `account-service/README.md`, `inventory-service/README.md`, `order-service/README.md`, `recommendation-service/README.md`

---

## Prompt 2

**Date:** 2026-10-08

**Goal:** Convert the final DOCX specification documents in `source-docs/` into clean Markdown specs under `specs/`, applying only three small consistency fixes, and link the specs from `README.md`.

**Exact user prompt:**

````text
We now have the final project specification documents in DOCX format.

Please use the existing DOCX files as the source material and create clean Markdown versions under:

specs/

Create:
- specs/requirements.md
- specs/domain-model.md
- specs/user-stories.md
- specs/api-endpoints.md
- specs/event-sourcing.md
- specs/cqrs.md
- specs/architecture.md

Rules:
- Preserve the existing terminology, structure, tables, IDs, names, and meaning.
- Do not invent new requirements, APIs, events, entities, or architecture.
- Do not rewrite content unnecessarily.
- Keep the Markdown clean and readable.
- Where the DOCX contains tables, reproduce them as Markdown tables.
- Where the DOCX contains diagrams/images, reference the existing image or add a short textual summary if needed.

Apply only these small consistency fixes:
1. Use one consistent service port mapping and API base path across specs and code.
2. Rewrite NFR-07 so it does not conflict with REST-based synchronous service calls:
   "Asynchronous inter-service communication shall use well-defined, versioned event schemas; synchronous reads may use documented REST APIs."
3. In CQRS, make sure ReleaseStockReservationCommand belongs to story I1, not I2.

If you find any other contradiction:
- do not silently change it
- report it first
- suggest the smallest possible fix

Also update README.md with links to the specs/ files.

Append this prompt and a short summary to PROMPT_HISTORY.md.

Do not implement business logic.
Do not commit or push.
````

**Summary of changes:**

- Converted the five DOCX files (`Non_Functional Requirements-2`, `Domain Model-3`, `API Endpoints-2`, `Event Sourcing Specification-2`, `CQRS Command and Query Specification-3`) and `architecture.drawio.png` into Markdown specs under `specs/`. Tables are reproduced as Markdown tables; diagrams are copied to `specs/images/` and referenced, with short textual summaries for the overview domain diagram and the architecture diagram.
- `specs/user-stories.md`: no user stories DOCX exists in `source-docs/`, so this file is only an index of the story IDs already referenced in the API Endpoints and CQRS specs (no stories invented).
- Fix 1 (ports/base path): the API Endpoints spec was taken as the source of truth — Catalog 8081, Inventory 8082, Order 8083, Account 8084, Recommendation 8085, base path `/api`. Updated `catalog-service` `application.yml` (port 8082 → 8081, context path `/api/v1` → `/api`), the service placeholder READMEs and the root `README.md` to match. `./mvnw clean test` still passes.
- Fix 2: NFR-07 rewritten to the wording given in the prompt.
- Fix 3: in the source CQRS DOCX, `ReleaseStockReservationCommand` was already mapped to I1; kept as I1.
- Other contradictions found were reported to the user (not changed).
- Added a "Specifications" section with links to all `specs/` files to `README.md`.

**Files created/modified:**

- `specs/requirements.md`, `specs/domain-model.md`, `specs/user-stories.md`, `specs/api-endpoints.md`, `specs/event-sourcing.md`, `specs/cqrs.md`, `specs/architecture.md` (created)
- `specs/images/*.png` (created – extracted from `Domain Model-3.docx` and copied from `architecture.drawio.png`)
- `catalog-service/src/main/resources/application.yml`
- `account-service/README.md`, `inventory-service/README.md`, `order-service/README.md`
- `README.md`
- `PROMPT_HISTORY.md`

---

## Prompt 3

**Date:** 2026-10-08

**Goal:** Apply the approved fixes for the specification contradictions reported after Prompt 2, and record the cart as an open architecture decision.

**Exact user prompt:**

````text
Apply fixes #1, #2, #3, #4, #5, #6, #7 and #9.

Decisions:
- OrderItem: use productId and unitPrice consistently.
- Add id:String as the primary identifier for User; username remains a separate field.
- I2 CQRS classification: Command (notification subscription) + event-driven projection.
- InventoryNotFound maps to ResourceNotFound (404).
- InventoryInitialized is created internally when an inventory record is first created for a product.
- StockChanged is an integration/umbrella event representing inventory stock changes and may be derived from StockLevelUpdated / StockLow / ProductRestocked.
- Use “Account Service” and “Recommendation Service” consistently.

Do not resolve #8 Cart yet. Mark it as an open architecture decision.

Also note that I will add the missing User Stories source document separately.

Update PROMPT_HISTORY.md.
Do not implement business logic.
Do not commit or push.
````

**Summary of changes:**

- #1 OrderItem: field renamed `productID` → `productId`; `unitPrice` kept. Added a note under the Order diagram that the diagrams' `price` label corresponds to `unitPrice`.
- #2 Domain class table: swapped the mislabelled column headers to `Domain Class | Description | Type`.
- #3 User: added `id: String` as the primary identifier; `username` kept as a separate required field. Added a note that the diagrams do not yet show `id`.
- #4 CQRS: I2 reclassified as "Command (notification subscription) + event-driven projection".
- #5 CQRS error model: noted that `InventoryNotFound` maps to `ResourceNotFound` (404).
- #6 Event Sourcing: noted that `InventoryInitialized` is created internally when an inventory record is first created for a product.
- #7 `StockChanged` documented as an integration/umbrella event that may be derived from `StockLevelUpdated` / `StockLow` / `ProductRestocked` (Architecture and Event Sourcing specs).
- #9 Renamed "Recommendations Service Domain Model" → "Recommendation Service Domain Model"; noted that the diagram's "User Service Domain" label refers to the Account Service.
- #8 Cart: not resolved; recorded as an open architecture decision in `specs/architecture.md`, referenced from `specs/api-endpoints.md` (Order Service) and `specs/user-stories.md` (O3).
- `specs/user-stories.md`: noted that the User Stories source document will be added separately.

**Files created/modified:**

- `specs/domain-model.md`
- `specs/cqrs.md`
- `specs/event-sourcing.md`
- `specs/architecture.md`
- `specs/api-endpoints.md`
- `specs/user-stories.md`
- `PROMPT_HISTORY.md`

---

## Prompt 4

**Date:** 2026-10-08

**Goal:** Apply final spec cleanups: consistent `UserProfile` class name and an I2 entry in the CQRS command catalog.

**Exact user prompt:**

````text
Apply these final spec cleanups:

1. Replace "User Profile" with "UserProfile" where it refers to the domain class.
2. Add an I2 command entry to the CQRS command catalog:
   - Command: SubscribeStockNotificationCommand
   - Story: I2
   - Target Aggregate: None
   - Output Event: StockNotificationSubscribed
   - Persistence: CRUD

Do not change anything else.
Do not implement business logic.
Append this prompt to PROMPT_HISTORY.md.
````

**Summary of changes:**

- Replaced "User Profile" with "UserProfile" in the Account Service row of the "Domain Classes and Link to User Stories" table (the only occurrence).
- Added `SubscribeStockNotificationCommand` (Target Aggregate: None, Story: I2, Output Events: StockNotificationSubscribed, Persistence: CRUD) to the CQRS command catalog, placed between the I1 and I3 rows.

**Files created/modified:**

- `specs/domain-model.md`
- `specs/cqrs.md`
- `PROMPT_HISTORY.md`

---

## Prompt 5

**Date:** 2026-10-08

**Goal:** Resolve the open Cart architecture decision, split prompt history into project-wide and per-service files, and run a final consistency review of the repository and `specs/` before microservice development starts.

**Exact user prompt:**

````text
Before we start full implementation, do one final cleanup and consistency review of the repository.

Please keep this simple and aligned with the university project scope. Do not over-engineer anything.

1. Resolve the open Cart architecture decision.

We are NOT creating a separate Cart aggregate or microservice.

For this project, the Order aggregate also represents the shopping cart before checkout.

Use a simple lifecycle such as:

DRAFT -> PLACED -> PROCESSING -> COMPLETED / CANCELLED

Meaning:
- A DRAFT order acts as the customer's cart.
- Products can be added/removed while the order is DRAFT.
- Checkout transitions the order from DRAFT to PLACED.
- After checkout, the normal order / inventory / Kafka flow begins.

Update the relevant specs so FR-09, the Order domain model, user stories and API contract are consistent with this decision.

Do not introduce a new Cart entity or service.

2. Prompt history structure.

Keep the existing root:

PROMPT_HISTORY.md

for project-wide prompts, setup, architecture and specification work.

Also create:

catalog-service/PROMPT_HISTORY.md
inventory-service/PROMPT_HISTORY.md
order-service/PROMPT_HISTORY.md
account-service/PROMPT_HISTORY.md
recommendation-service/PROMPT_HISTORY.md

These files will contain AI prompts related only to that microservice.

Add a short heading/explanation to each file, but do not invent old prompts.

From now on:
- project-wide work -> root PROMPT_HISTORY.md
- service-specific work -> that service's PROMPT_HISTORY.md

3. Final repository review.

Review the current repository and all files under specs/.

Check for:
- inconsistent names
- service ports
- /api base path
- domain class names
- user story IDs
- API endpoint mismatches
- event names
- CQRS / event sourcing inconsistencies
- README inconsistencies
- architecture inconsistencies

Do NOT silently redesign anything.

If you find another real contradiction:
- report it
- explain it simply
- suggest the smallest fix

Only automatically fix very small obvious issues such as spelling, naming or formatting.

4. Repository structure.

Keep:
- README files inside each microservice
- specs/images/
- source-docs/ for now

Do not delete source-docs yet.

Do not implement business logic.
Do not add new libraries.
Do not commit or push.

Append this prompt to the root PROMPT_HISTORY.md.

At the end give me a short report:
1. what you changed
2. whether the Cart decision is now consistent across specs
3. any remaining contradictions
4. whether the repository is ready to commit and begin microservice development
````

**Summary of changes:**

- Cart decision resolved (no Cart entity/service; a `DRAFT` Order is the cart):
  - `specs/requirements.md`: FR-09 now says "a cart (a DRAFT order)"; added a cart note below the FR table.
  - `specs/domain-model.md`: Order status values changed to `DRAFT/PLACED/PROCESSING/COMPLETED/CANCELLED`; added section 3.3 Order Lifecycle; Order class description mentions the DRAFT cart.
  - `specs/api-endpoints.md`: O1 `POST /orders` now creates a DRAFT order; added O1 `POST /orders/{orderId}/items`, `DELETE /orders/{orderId}/items/{orderItemId}` and `POST /orders/{orderId}/checkout`; O7 creates a DRAFT order from a build; added a cart note.
  - `specs/architecture.md`: "Open Architecture Decisions" became "Architecture Decisions" with Cart marked Resolved; noted that `OrderCreated` is published on checkout (DRAFT → PLACED).
  - `specs/user-stories.md`: O1 lists the new cart/checkout endpoints; status note updated because `source-docs/User Stories-2.docx` now exists (conversion still pending). Initially the cart item endpoints were placed under O3, which was corrected to O1 after the new source document showed that O3 is "Update Order Fulfilment Status".
  - `order-service/README.md`: noted that the cart is a DRAFT Order.
- Prompt history: created `PROMPT_HISTORY.md` in each of the five service directories (heading and explanation only); root `PROMPT_HISTORY.md` intro and root `README.md` now explain the split.
- Small fixes: InventoryItem.productId description "Catalog Class" → "Catalog Service"; Order.userId description "placed" → "placed by"; added the missing `catalog-service/README.md`.
- Review: ports (8081–8085) and `/api` base path are consistent across specs, READMEs and `catalog-service` config; all relative links/anchors in specs resolve; `./mvnw clean test` passes. Remaining contradictions were reported to the user, not changed.

**Files created/modified:**

- `specs/requirements.md`, `specs/domain-model.md`, `specs/api-endpoints.md`, `specs/architecture.md`, `specs/user-stories.md`
- `catalog-service/PROMPT_HISTORY.md`, `inventory-service/PROMPT_HISTORY.md`, `order-service/PROMPT_HISTORY.md`, `account-service/PROMPT_HISTORY.md`, `recommendation-service/PROMPT_HISTORY.md` (created)
- `catalog-service/README.md` (created)
- `order-service/README.md`
- `README.md`
- `PROMPT_HISTORY.md`

---

## Prompt 6

**Date:** 2026-10-08

**Goal:** Resolve the remaining Order ↔ Inventory contradictions A–E reported after Prompt 5, using the suggested smallest fixes.

**Exact user prompt:**

````text
Complete remeining contradictions A - E
````

**Summary of changes:**

- A (reservation timing): stock is reserved after checkout (order `PLACED`) when Inventory Service consumes `OrderPlaced`; the FR-06 / I1 time limit applies to payment — if payment is not recorded in time the order is `CANCELLED` and the reservation released. Documented in the Order Lifecycle, an FR-06 note, and Architecture Decision 2.
- B (reservation REST endpoints): the normal flow reserves/releases stock via Kafka; the I1 REST endpoints are kept for direct use and testing (note in the API spec, Architecture Decision 5, CQRS command application service).
- C (event names): the checkout Kafka event is `OrderPlaced` (replaces `OrderCreated`); `CheckoutStarted` is not used (Architecture Decision 3; note that the diagram still says `OrderCreated`).
- D (cancellation): added `OrderCancelled` Kafka flow (Order → Inventory), which triggers `ReleaseStockReservationCommand`.
- E (lifecycle transitions): added a transitions table to the Order Lifecycle: `StockReserved` → PROCESSING, `StockReservationFailed` → CANCELLED, successful payment → COMPLETED, failed/late payment → CANCELLED, customer cancel from DRAFT/PLACED/PROCESSING. Added the `StockReservationFailed` Kafka flow (Inventory → Order; published, not event-sourced). Extended the `OrderStateViolation` note in the API spec.

**Files created/modified:**

- `specs/domain-model.md`
- `specs/architecture.md`
- `specs/api-endpoints.md`
- `specs/cqrs.md`
- `specs/requirements.md`
- `PROMPT_HISTORY.md`

---

## Prompt 7

**Date:** 2026-10-08

**Goal:** Convert the User Stories DOCX into the final `specs/user-stories.md`, using the event names agreed in the Architecture Decisions.

**Exact user prompt:**

````text
Next step convert User Stories DOCX into final specs/user-stories.md with new event names that were setted up
````

**Summary of changes:**

- Replaced the temporary story index in `specs/user-stories.md` with the full content of `source-docs/User Stories-2.docx`: the User Story Mapping table (domain classes and triggering domain events) and every story (statement plus acceptance rules or scenario), grouped by service.
- Event names updated to the agreed ones: O1 `CheckoutStarted, OrderPlaced` → `OrderPlaced`; O3 `OrderCreated, StockReserved` → `OrderPlaced, StockReserved, StockReservationFailed`.
- Typo fixes only: story headings `L2`/`L3` → `I2`/`I3`; `C5.` → `C5:`; I1 step "stock is reservation is released" → "the stock reservation is released". Story text is otherwise unchanged.
- Added short "Agreed flow" notes under I1, O1, O3 and O4 where the story wording differs from the agreed order/inventory flow (Architecture Decisions 1–4).
- Kept a "User Story to API Endpoint Mapping" section; the `POST /orders/from-build/{buildId}` endpoint (API Feature ID `O7`) is mapped to story R3.
- Updated the User Stories link description in `README.md`.

**Files created/modified:**

- `specs/user-stories.md`
- `README.md`
- `PROMPT_HISTORY.md`

---

## Prompt 8

**Date:** 2026-10-08

**Goal:** Final spec cleanup before implementation: relabel the from-build endpoint as R3 and add the domain fields/classes already required by the specs.

**Exact user prompt:**

````text
Make the final spec cleanup before implementation:

1. In `specs/api-endpoints.md`, change the `O7` label for `POST /orders/from-build/{buildId}` to `R3`.

2. Update `specs/domain-model.md` to add the missing domain fields/classes already required by the specs:
- `Review`
- `StockNotificationSubscription`
- `Order.deliveryAddressId`
- `Order.paymentStatus`
- `Address.id`
- a credential/password field or simple credential model for User authentication
- `BuildComponent.id`

Keep it simple and consistent with the existing domain model. Do not invent extra features.

Do not change Kafka/event decisions unless needed for consistency.

Update `PROMPT_HISTORY.md`.

Do not implement business logic.
Do not commit or push.

At the end, briefly report what changed and whether any contradictions remain.
````

**Summary of changes:**

- `specs/api-endpoints.md`: `POST /orders/from-build/{buildId}` relabelled from `O7` to `R3`.
- `specs/domain-model.md`:
  - New `1.4 Review` (id, productId, userId, rating, comment, createdAt) – Aggregate Root / Entity in Catalog Service.
  - New `2.3 StockNotificationSubscription` (id, productId, userId, createdAt) – Aggregate Root / Entity in Inventory Service, CRUD (not part of the event-sourced InventoryItem).
  - `Order.deliveryAddressId` (Optional) and `Order.paymentStatus` (PENDING/PAID/FAILED); the Order Lifecycle payment transitions now reference `paymentStatus`.
  - `User.passwordHash` (simple credential field for A2), `Address.id`, `BuildComponent.id`.
  - Both new classes added to the service → classes table and the domain class table; added a note that the diagrams do not show the additions.
- `specs/user-stories.md`: mapping table now lists `Review` for C4 and `StockNotificationSubscription` for I2; endpoint mapping note updated to the `R3` label.
- No Kafka/event decisions changed.

**Files created/modified:**

- `specs/api-endpoints.md`
- `specs/domain-model.md`
- `specs/user-stories.md`
- `PROMPT_HISTORY.md`

---

## Prompt 9

**Date:** 2026-10-08

**Goal:** Final spec cleanup before committing the baseline: apply the remaining small decisions, add a root `CLAUDE.md` with project rules, and run a final consistency review.

**Exact user prompt:**

````text
Do the final spec cleanup before we commit the baseline.

Apply these decisions:

1. Change the User–Address relationship to:
   User 1 -> 0..* Address

2. Keep Order paymentStatus values as:
   PENDING / PAID / FAILED

3. Define StockReservation.status values as:
   ACTIVE / RELEASED / CONFIRMED

4. In event sourcing, use InventoryItem.id as aggregateId.
   Keep productId as a separate field in the event payload/model.

5. Rename the R1 API request DTO from BuildRequest to BuildRecommendationRequest to avoid confusion with the BuildRequest domain class.

6. In the domain model table, rename the column "User Story" to "Related Requirements" because it contains FR IDs.

7. Create a root CLAUDE.md with the project rules:
   - specs/ is the source of truth
   - exactly 5 microservices
   - Java 21 / Spring Boot / JPA / H2 / Kafka / LangChain4j
   - follow layered architecture and DDD
   - do not over-engineer
   - do not invent requirements
   - report conflicts instead of silently resolving them
   - do not commit or push unless explicitly asked
   - root PROMPT_HISTORY.md for project-wide work
   - per-service PROMPT_HISTORY.md for service-specific work

8. Run one final consistency review across:
   - specs
   - README files
   - service ports
   - API paths
   - domain names
   - events
   - CQRS
   - event sourcing
   - architecture

Only fix obvious naming/formatting issues automatically.
If you find a real architectural contradiction, report it instead of redesigning anything.

Update the root PROMPT_HISTORY.md with this prompt.

Do not implement business logic.
Do not commit or push.

At the end tell me only:
- what changed
- any remaining contradictions
- whether the repository is ready to commit and start development
````

**Summary of changes:**

- `specs/domain-model.md`: User–Address is now User 1 — 0..* Address (note added that the diagrams still show 1); `StockReservation.status` values ACTIVE/RELEASED/CONFIRMED; column "User Story" renamed to "Related Requirements" and the section heading to "Domain Classes and Link to Requirements". `Order.paymentStatus` values PENDING/PAID/FAILED kept.
- `specs/event-sourcing.md`: `aggregateId` is `InventoryItem.id`, with `productId` kept separately in the payload; stream key example updated to `InventoryItem:inventory-item-123`.
- `specs/api-endpoints.md`: R1 request body renamed `BuildRequest` → `BuildRecommendationRequest`.
- Created root `CLAUDE.md` with the project rules (also includes the service/port table and the "no AI Git trailers" rule from Prompt 1).
- Final review: ports, `/api` base path, story IDs vs API feature IDs, event names, CQRS and event-sourcing events are consistent. Three architectural contradictions were reported to the user, not changed: no flow reaches `StockReservation` CONFIRMED; C2's Catalog → Inventory query is missing from the architecture REST calls; R3's `POST /orders/from-build/{buildId}` needs build data from Recommendation Service but there is no Order → Recommendation call or endpoint by `buildId`.

**Files created/modified:**

- `specs/domain-model.md`
- `specs/event-sourcing.md`
- `specs/api-endpoints.md`
- `CLAUDE.md` (created)
- `PROMPT_HISTORY.md`

---

## Prompt 10

**Date:** 2026-10-08

**Goal:** Resolve the three architectural contradictions reported after Prompt 9, using the suggested smallest fixes.

**Exact user prompt:**

````text
Complete contradictions 1 - 3
````

**Summary of changes:**

- 1 (StockReservation CONFIRMED unreachable): when payment is PAID (order → COMPLETED), Order Service publishes `OrderCompleted` (new Kafka flow Order → Inventory). Inventory Service issues `ConfirmStockReservationCommand` (I1), which records the new event-sourced event `StockReservationConfirmed`: reservation → CONFIRMED, reserved quantity deducted from stock. Added quantity effects of reservation events and the status ↔ event mapping for `StockReservation.status`. Architecture Decision 6.
- 2 (C2 stock status): added the Catalog Service → Inventory Service REST call (`GET /inventory/{productId}`) to the architecture. Architecture Decision 8.
- 3 (R3 order from build): endpoint changed from `POST /orders/from-build/{buildId}` to `POST /orders/from-recommendation/{recommendationId}`; added the Order Service → Recommendation Service REST call (`GET /recommendations/{recommendationId}`). Architecture Decision 7.
- Noted that the architecture diagram does not show the new REST calls or `OrderCompleted`.

**Files created/modified:**

- `specs/architecture.md`
- `specs/domain-model.md`
- `specs/event-sourcing.md`
- `specs/cqrs.md`
- `specs/api-endpoints.md`
- `specs/user-stories.md`
- `PROMPT_HISTORY.md`

---

## Prompt 11

**Date:** 2026-10-08

**Goal:** Final review of all changes and inconsistency fixes made during the specification phase, and a readiness check for the microservice implementation stage.

**Exact user prompt:**

````text
Now check all the changes and inconsistences we fixed for this session, and in project. Finalize with short summary and is the project ready to move to implementing microservices stage
````

**Summary of changes:**

- Re-read all `specs/` files end to end and cross-checked them with the README files, `CLAUDE.md`, `PROMPT_HISTORY.md` files and the `catalog-service` configuration: ports (8081–8085), `/api` base path, domain names, user story IDs vs API feature IDs, event names, CQRS, event sourcing, order lifecycle and architecture decisions.
- No contradictions found and no files changed during the review. `./mvnw clean test` passes.
- Noted for implementation (not contradictions): the draw.io diagrams are outdated (differences are noted in the specs); Kafka event payload schemas (NFR-07) are to be defined when implementing order-service and inventory-service; `Product` has no `categoryId` field although Product 0..* — 1 Category.
- Conclusion: the project is ready to commit the baseline and start microservice implementation, beginning with catalog-service and account-service.

**Files created/modified:**

- `PROMPT_HISTORY.md` (this entry only)

---

## Prompt 12

**Date:** 2026-10-10

**Goal:** Resolve the catalog-service spec gaps found while reading the specs on the `catalog-microservice` branch, so that catalog-service can be implemented.

**Exact user prompt:**

````text
Resolve all the spec gaps. Provide a summary of what was changed and where. Also inform me if the catalog service is ready to be implemented after resolving the spec gaps.
````

**Summary of changes:**

- Product ↔ Category / Specification: added `Product.categoryId` (Required) and `Product.specifications` (at least one) to the domain model, recording the associations already shown in the diagram.
- Catalog DTOs: defined the fields of `ProductCreateRequest`, `ProductUpdateRequest`, `ProductResponse`, `CategoryCreateRequest`, `CategoryUpdateRequest`, `CategoryResponse`, `ReviewCreateRequest` and `ReviewResponse` in the API spec, plus the `GET /products` query parameters (`categoryId`, `brand`, `specName` + `specValue`). `ProductResponse.stockStatus` (`IN_STOCK` / `OUT_OF_STOCK` / `UNKNOWN`) is set only for C2.
- Listing categories: added `GET /categories` (C1) to the API spec and the user story endpoint mapping (Architecture Decision 10).
- Catalog events: added section 1.5 Catalog Domain Events with payload fields. Only `ProductViewed` is published to Kafka and Catalog Service consumes it for the trending list; `ProductCreated`, `ProductUpdated`, `ProductRemoved` and `ProductReviewed` are internal domain events (Architecture Decision 9).
- Package name: `CLAUDE.md` now states that the package is `com.csci318.<service>` without the hyphen, matching the existing `com.csci318.catalogservice`.
- No code changed. Recorded here rather than in `catalog-service/PROMPT_HISTORY.md` because the changes are to the shared specs and `CLAUDE.md`.

**Files created/modified:**

- `specs/domain-model.md`
- `specs/api-endpoints.md`
- `specs/architecture.md`
- `specs/user-stories.md`
- `CLAUDE.md`
- `PROMPT_HISTORY.md`


---

## Prompt 13

**Date:** 2026-10-10

**Goal:** Commit the catalog spec gap fixes from Prompt 12 and check that catalog-service is ready for C5 implementation.

**Exact user prompt:**

````text
Make a commit before implementing C5. Inform me of the changes and where. Perform a check to ensure catalog service is ready to be implemented after the implementations from this prompt.
````

**Summary of changes:**

- Committed the Prompt 12 spec changes on the `catalog-microservice` branch (not pushed).
- Readiness check: `./mvnw clean test` passes; the new spec links and anchors resolve; ports, `/api` base path and package name match the specs. No spec or code changes were made by the check.
- Noted for C5: `spring-boot-starter-validation` is not yet a dependency of `catalog-service` (needed for `ValidationFailed` 400); behaviour when removing a category that still has products is undefined; no admin authorisation mechanism is specified.

**Files created/modified:**

- `PROMPT_HISTORY.md` (this entry only)

---

## Prompt 14

**Date:** 2026-10-10

**Goal:** Project-wide parts of the catalog-service work recorded as Prompt 2 in `catalog-service/PROMPT_HISTORY.md` (fix the issues left after C5, implement C1).

**Exact user prompt:**

````text
Fix the unresolved issues and Implement C1.
````

**Summary of changes:**

- `specs/api-endpoints.md`: added `CategoryInUse` (409 Conflict, Catalog) to the error response contract.
- `specs/architecture.md`: added Architecture Decision 11 (admin authorisation) with status Open; the user chose to leave it undecided for now.
- `README.md`: catalog-service status changed from "skeleton" to "C1, C5 implemented".

**Files created/modified:**

- `specs/api-endpoints.md`
- `specs/architecture.md`
- `README.md`
- `PROMPT_HISTORY.md`

---

## Prompt 15

**Date:** 2026-10-10

**Goal:** Project-wide parts of the catalog-service work recorded as Prompt 3 in `catalog-service/PROMPT_HISTORY.md` (commit C5 and C1, implement C4).

**Exact user prompt:**

````text
Leave admin authorisation for after account microservice is implemented. Commit C5 and C! work. For C4, the rating range should be 1-5 stars. Implement C4.
````

**Summary of changes:**

- `specs/domain-model.md`: `Review.rating` constraint is now "Required, 1–5" (stars).
- `specs/api-endpoints.md`: `ReviewCreateRequest.rating` noted as 1–5.
- `specs/architecture.md`: Architecture Decision 11 (admin authorisation) stays Open and is marked as deferred until Account Service is implemented.
- `README.md`: catalog-service status changed to "C1, C4, C5 implemented".

**Files created/modified:**

- `specs/domain-model.md`
- `specs/api-endpoints.md`
- `specs/architecture.md`
- `README.md`
- `PROMPT_HISTORY.md`

---

## Prompt 16

> Recorded as Prompt 12 on `feature/account-service` before `origin/main` was merged (renumbered to follow the Prompts 12–15 from `main`).

**Date:** 2026-10-08

**Goal:** Update the root README status of account-service after implementing A1 registration and user lookup (service work is recorded in `account-service/PROMPT_HISTORY.md`, Prompt 2).

**Exact user prompt:**

````text
Lets move to A1 now.

Implement basic user registration and user lookup:
- User aggregate with UserProfile
- repository and service
- POST /users
- GET /users/{userId}
- use BCrypt for password hashing with spring-security-crypto
- add basic 400/404 handling and tests

Keep UserRegistered and UserProfileUpdated as internal domain events for now, not Kafka.

Also update account-service README and root README so they no longer say account-service is a placeholder.

Follow CLAUDE.md and specs.
Run relevant tests.
Update account-service/PROMPT_HISTORY.md for the service work and root PROMPT_HISTORY.md only for the root README change.
Do not commit or push.
````

**Summary of changes:**

- `README.md`: account-service status changed from `placeholder` to `in progress (A1 registration + user lookup)`.

**Files created/modified:**

- `README.md`
- `PROMPT_HISTORY.md`

---

## Prompt 17

> Recorded as Prompt 13 on `feature/account-service` before `origin/main` was merged.

**Date:** 2026-10-12

**Goal:** Update the root README status of account-service now that A1 is complete (service work is recorded in `account-service/PROMPT_HISTORY.md`, Prompts 2–6).

**Exact user prompt:**

````text
A1 is completed. Update the root README.md status of Account Service to reflect that A1 is complete.
Do not change any implementation or specs. Update the root PROMPT_HISTORY.md for this change as required by the project rules.
Do not commit or push.
````

**Summary of changes:**

- `README.md`: account-service status changed from `in progress (A1 registration + user lookup)` to `in progress (A1 complete; A2, A3 pending)`. It stays "in progress" because A2 (authentication) and A3 (preferences) are not implemented yet.
- No implementation or spec changes.

**Files created/modified:**

- `README.md`
- `PROMPT_HISTORY.md`

---

## Prompt 18

**Date:** 2026-10-12

**Goal:** Agree the A2 authentication design (JWT + Spring Security), record it in the Account/API/architecture specs and close Architecture Decision 11. Specs only — no implementation.

**Exact user prompt:**

````text
Consider a JWT/Spring Security solution for A2. Update only the required Account/API/architecture specs and close Decision 11 with the agreed authentication design. Do not write any implementation yet. Update the root PROMPT_HISTORY.md as required. Do not commit or push.
````

**Decisions confirmed by the user during this prompt:**

- Administrators: add `role` (CUSTOMER/ADMIN) to `User`; `POST /users` always creates CUSTOMER; one ADMIN account is created by Account Service at startup from configuration.
- Scope: the JWT design applies to all services — every service validates the token with the shared secret; service-to-service calls forward the customer's token.

The other choices follow the defaults proposed in the A2 review (`account-service/PROMPT_HISTORY.md`, Prompt 7).

**Summary of changes:**

- `specs/architecture.md`:
  - Decision 11 renamed to "Authentication and authorisation (A2, NFR-04)" and changed from Open to **Resolved**. It now describes the design:
    - login with username + password checked against the BCrypt hash;
    - stateless HS256 JWT (shared secret, 1-hour lifetime, claims `sub`, `role`, `iat`, `exp`, no refresh/logout/session);
    - every service validates tokens with Spring Security as an OAuth2 resource server;
    - CUSTOMER/ADMIN roles with a seeded admin;
    - owner check on customer endpoints, ADMIN on C5, I3 `PUT` and the I1 REST endpoints;
    - the `Authorization` header is forwarded on synchronous calls.
  - "Synchronous REST Calls": added a note that calls to protected endpoints forward the customer's JWT.
- `specs/api-endpoints.md`:
  - New "Account Service DTOs (A2)" table: `LoginRequest` (username, password); `AuthenticationResponse` (token, tokenType `Bearer`, expiresAt, userId, role). Also a note on the login errors: 400 for missing fields; 401 for an unknown user or a wrong password, with the same message.
  - New "Authentication and Authorisation" section:
    - token format and the `Authorization: Bearer` header;
    - Public / Customer (owner) / Admin access table covering every endpoint in all five services;
    - 401/403 rules, and when an owner check gives 403 rather than 404;
    - token forwarding for inter-service calls; `GET /inventory/{productId}` stays public for C2; the H2 console is not protected.
  - The contract rule on account-specific endpoints now links to the new section. The error-contract rows for `AuthenticationRequired` (now also for `POST /auth/login`) and `AccessDenied` are more precise.
- `specs/domain-model.md`: `User.role` (String, CUSTOMER/ADMIN, Required) added to 4.1. Both "not shown in the diagrams" notes now list `User.role`.
- `user-stories.md`, `requirements.md`, `cqrs.md` and `event-sourcing.md` are unchanged: A2 still uses `User` only and has no domain events.
- No implementation changes.

**Files created/modified:**

- `specs/architecture.md`
- `specs/api-endpoints.md`
- `specs/domain-model.md`
- `PROMPT_HISTORY.md`

---

## Prompt 19

**Date:** 2026-10-13

**Goal:** Record the agreed A3 (Save Recommendation Preferences) API decisions in `specs/api-endpoints.md` as an Account Service DTO table. Specs only — no implementation.

**Exact user prompt:**

````text
Add the A3 DTO table to specs/api-endpoints.md and add to root PROMPT_HISTORY.md
For D3 not set fake limitation 1000. For free-text preference better to save string as TEXT/CLOB without API validation for length.
Don't implement A3 yet. This step is only for updating the API spec and recording the decision.
````

**Decisions (from the A3 review, `account-service/PROMPT_HISTORY.md` Prompts 10–11, with D3 revised here):**

| # | Decision |
|---|---|
| D1 | `PreferenceUpdateRequest` = `{ "preference" }`; `PreferenceResponse` = `{ "userId", "preference" }`. |
| D2 | `Preference` stays one free-text String (domain-model.md 4.3); no structured fields. |
| D3 (revised) | No maximum length and no length validation in the API. The value is stored as a large text column (TEXT/CLOB, e.g. `@Lob` / `columnDefinition`), so a long value is not cut off by the default `varchar(255)`. No artificial 1000-character limit. This replaces the note in `account-service/PROMPT_HISTORY.md` Prompt 11 that a long value would return 500. |
| D4 | `GET` for an existing user with no saved preference → 200 with `preference: null`; unknown user → 404 `ResourceNotFound`. |
| D5 | `PUT` with a missing, `null` or blank `preference` clears it. |
| D6 | `PreferenceUpdated` is raised only when the value changes; internal domain event, not Kafka. |
| D7 | The preference is not part of `UserResponse`. |
| D8 | Recommendation Service integration is deferred to the recommendation-service implementation. |

**Summary of changes:**

- `specs/api-endpoints.md`: new "Account Service DTOs (A3)" table after the A2 table: `PreferenceUpdateRequest` (preference, free-text String, optional) and `PreferenceResponse` (userId, preference, `null` when none is saved). A note covers D4, D5, the missing length limit (D3) and D6.
- The TEXT/CLOB storage choice (D3) is an implementation detail and is recorded here only, not in the specs.
- `domain-model.md`, `user-stories.md`, `architecture.md`, `requirements.md`, `cqrs.md` and `event-sourcing.md` are unchanged: they already define `Preference` (one optional String), the A3 endpoints, `PreferenceUpdated` and the owner access rule; `PreferenceUpdated` is not in the Kafka flows.
- No implementation changes.

**Files created/modified:**

- `specs/api-endpoints.md`
- `PROMPT_HISTORY.md`

---

## Prompt 20

**Date:** 2026-10-14

**Goal:** Record the agreed Recommendation Service decisions in the specs (DTOs, error handling, component types, compatibility rules, data access). Specs only — no implementation.

**Exact user prompt:**

````text
Update the Recommendation specs only, no implementation yet.
Add Recommendation DTOs to specs/api-endpoints.md based on our decisions: R1/R3/R4 response should include recommendationId, buildId, userId, budget, purpose, preferences, totalPrice, description, reasoning, and components[] with productId, componentType, price. R2 request is productIds[], response is compatible + conflicts[].
Record these decisions too: Catalog unavailable → 503, empty R2 product list → 400, unknown productId → 404, Inventory 404 means unavailable/out of stock, Inventory unavailable → 503.
Define canonical component categories: CPU, Motherboard, RAM, GPU, Storage, PSU, Case.
Define compatibility spec names: Socket, Memory Type, Form Factor.
R2 deterministic checks: CPU↔Motherboard Socket, RAM↔Motherboard Memory Type, Motherboard↔Case Form Factor. No wattage rule for now.
Use GET /products and map locally; Recommendation calls Inventory directly for stock.
Add the decisions to root PROMPT_HISTORY.md. Don’t implement Recommendation yet. Don’t commit or push.
````

**Decisions** (this prompt, plus RD1–RD8 from `recommendation-service/PROMPT_HISTORY.md` Prompt 2):

| # | Decision |
|---|---|
| R-D1 | `BuildRecommendationRequest` = `userId, budget, purpose, preferences?`. |
| R-D2 | `RecommendationResponse` (R1, R3, R4) = `recommendationId, buildId, userId, budget, purpose, preferences, totalPrice, description, reasoning, components[{productId, componentType, price}]`. R4 returns an array of it. |
| R-D3 | `CompatibilityRequest` = `productIds[]`; `CompatibilityResponse` = `compatible, conflicts[]`. R2 always returns 200 (`compatible: true`, or `false` + conflicts); `CompatibilityConflict` (409) is not used by R2. |
| R-D4 | Errors: empty R2 product list → 400 `ValidationFailed`; unknown `productId` → 404 `ResourceNotFound`; Account, Catalog or Inventory unavailable → 503 `ServiceUnavailable`; Inventory 404 (no record) → product not available. |
| R-D5 | Canonical component categories (Catalog category names = `componentType`): CPU, Motherboard, RAM, GPU, Storage, PSU, Case. |
| R-D6 | Compatibility spec names: Socket, Memory Type, Form Factor. |
| R-D7 | R2 deterministic rules: CPU ↔ Motherboard `Socket`, RAM ↔ Motherboard `Memory Type`, Motherboard ↔ Case `Form Factor`. No wattage rule. |
| R-D8 | Recommendation reads products with Catalog `GET /products` and maps them locally (not `GET /products/{productId}`); it calls Inventory `GET /inventory/{productId}` directly for stock. Only in-stock products can be recommended. |
| R-D9 | R1 AI flow: Java collects saved Account preference + request preference (separate inputs, request wins), Catalog products and stock; LangChain4j sends the context to the LLM, which returns product IDs + reasoning; Java re-checks IDs, prices (from Catalog), budget and stock. No tool-calling for now. Provider/model/API key from config/env; tests mock the LLM. |
| R-D10 | BuildRequest and PCBuild stay separate aggregate roots; the domain-model diagram's composition is out of date. |
| R-D11 | `BuildRequested`, `RecommendationGenerated`, `CompatibilityChecked` are internal domain events (no Kafka). R1/R3/R4 protected (owner), R2 public. |

**Summary of changes:**

- `specs/api-endpoints.md`: new "Recommendation Service DTOs" table (R-D1–R-D3) and notes on R2 status codes, data access, stock and 503 handling (R-D3, R-D4, R-D8).
- `specs/domain-model.md`: `BuildComponent.componentType` now refers to the canonical types; new section 5.5 "Component Types and Compatibility Rules" (R-D5–R-D7); note that BuildRequest/PCBuild are separate aggregate roots and the diagram is out of date (R-D10).
- `specs/architecture.md`: Architecture Decision 12 (R-D4, R-D8, R-D9, R-D11).
- `specs/user-stories.md`: note under R2 that step 3 uses deterministic rules, not the AI agent; story text unchanged.
- Assumption added to the spec: each `conflicts[]` item is `{specName, productIds, message}` (structure was not specified).
- No implementation changes.

**Open issues (not decided, not in specs):**

- R2 rule edge cases: a rule whose two component types are not both in the request; a product missing the spec; several products of one type; products outside the canonical categories; value comparison exact or case-insensitive; a case that supports several form factors.
- R1: which component types are required (all seven? GPU optional?); error when no valid build fits the budget or when the LLM fails/times out; request validation (budget > 0, purpose non-blank).
- Whether `BuildRequest.preferences` stores only the request preference (assumed) or also the saved Account preference.
- R4 order: no `createdAt` on BuildRequest/Recommendation.
- `CompatibilityConflict` (409) is now used by no endpoint; FR-19 is tagged "agentic AI" while R2 is now deterministic.
- User Story Mapping still lists PCBuild/BuildComponent for R2 although R2 has no BuildRequest to attach a PCBuild to.
- Recommendation domain-model diagram to be updated; Catalog demo data must use the canonical category and spec names.

**Files created/modified:**

- `specs/api-endpoints.md`
- `specs/domain-model.md`
- `specs/architecture.md`
- `specs/user-stories.md`
- `PROMPT_HISTORY.md`

---

## Prompt 21

**Date:** 2026-10-14

**Goal:** Resolve the Recommendation open issues from Prompt 20 (R2 rule edge cases, R1 validation and errors, stored preference, R4 order, R2 persistence) and record them in the specs. Specs only — no implementation.

**Exact user prompt:**

````text
If for one compatibility rule is missing one of the two component types - skip the rule, its not a conflict. If both components exist, but one is missing the required specification - compatible:false, conflict with message about missing specification. Two products with same canonical type - 400 ValidationFailed. Product is outside the canonical categories - 400 ValidationFailed. Compare specification values using trim + case-insensitive matching. For now not trying to parse ATX / mATX and other free-text values: demo Catalog data must use one canonical value. R1 needs all 7 types: CPU, Motherboard, RAM, GPU, Storage, PSU, Case. budget > 0, purpose non-blank, preferences optional. If its not possible to make valid build within the budget - use 409 CompatibilityConflict. LLM timeout/error - 503 ServiceUnavailable. In BuildRequest.preferences store only preference from current request, the saved Account preference is used only as additional LLM context. R4 for now without guaranteed sort order. R2 not using PCBuild and saves nothing. FR-19 keep the same for now: AI is still in R1 and R2 does not use PCBuild and persists nothing. Update the relevant specs and root PROMPT_HISTORY.md with these decisions. Do not implement Recommendation yet. Do not commit or push.
````

**Decisions:**

| # | Decision |
|---|---|
| R-D12 | A compatibility rule whose two component types are not both selected is skipped (not a conflict). |
| R-D13 | Both component types present but one product lacks the rule's specification → `compatible: false`, with a conflict whose message names the missing specification. |
| R-D14 | R2: two products of the same canonical type → 400 `ValidationFailed`; a product outside the canonical categories → 400 `ValidationFailed`. |
| R-D15 | Specification values are compared with trim + case-insensitive matching; values are not parsed (no ATX/mATX logic). Catalog demo data must use one canonical value per specification. |
| R-D16 | An R1 build needs all seven types: CPU, Motherboard, RAM, GPU, Storage, PSU, Case. |
| R-D17 | R1 validation: `budget` > 0, `purpose` not blank, `preferences` optional (else 400 `ValidationFailed`). |
| R-D18 | No valid build possible within the budget → 409 `CompatibilityConflict`. LLM error or timeout → 503 `ServiceUnavailable`. |
| R-D19 | `BuildRequest.preferences` stores only the current request's preference; the saved Account preference is only extra LLM context. |
| R-D20 | R4 has no guaranteed sort order for now. |
| R-D21 | R2 does not use PCBuild and stores nothing. FR-19 stays unchanged; the AI is used in R1. |

**Summary of changes:**

- `specs/api-endpoints.md`: R2 note adds the 400 cases and "stores nothing" (R-D14, R-D21); new R1 note on validation, stored preference, 409 and 503 (R-D16–R-D19); R4 note on order (R-D20). Error contract: `CompatibilityConflict` meaning changed to "no valid PC build can be generated (R1); not used by R2" (R-D18); `ServiceUnavailable` also covers Recommendation LLM calls (R-D18).
- `specs/domain-model.md`: BuildRequest constraints `budget` > 0, `purpose` not blank, `preferences` = request preference only (R-D17, R-D19); section 5.5 adds the seven-type build, rule skipping, missing-specification conflict, trim + case-insensitive comparison, canonical demo values, and the R2 400 cases (R-D12–R-D16, R-D21).
- `specs/architecture.md`: Decision 12 adds R1 503 (LLM) / 409 (no valid build), and R2 uses no LLM and stores nothing.
- `specs/user-stories.md`: R2 mapping is now `Product, Specification` (was `PCBuild, BuildComponent, Product, Specification`); R2 note says the AI is used in R1 and R2 stores nothing. Story text and `requirements.md` (FR-19) unchanged.
- Assumption added to the spec: the R2 compatibility rules also apply to the build generated in R1 ("valid build" = compatible, in stock, within budget), since a failure is reported as `CompatibilityConflict`.
- No implementation changes.

**Still open:**

- R1: whether an invalid LLM answer (unknown IDs, over budget, incompatible) is retried, or returned directly as 409/503.
- FR-19 is still tagged "agentic AI" although R2 is rule-based (kept on purpose for now).
- LLM provider/model (depends on the API key); Recommendation domain-model diagram update; Catalog demo data with canonical categories, spec names and values.

**Files created/modified:**

- `specs/api-endpoints.md`
- `specs/domain-model.md`
- `specs/architecture.md`
- `specs/user-stories.md`
- `PROMPT_HISTORY.md`

---

## Prompt 22

**Date:** 2026-10-14

**Goal:** Decide how R1 validates the LLM answer (compatibility rules, retry) and record it in the specs. Specs only — no implementation.

**Exact user prompt:**

````text
R1 must use the same compatibility rules as R2.
If the LLM output is invalid, retry once with the validation feedback. If the second result is also invalid, return 409 CompatibilityConflict.
If the LLM call fails technically or times out, return 503 ServiceUnavailable.
Update the relevant specs and root PROMPT_HISTORY.md. Do not implement anything yet. Do not commit or push.
````

**Decisions:**

| # | Decision |
|---|---|
| R-D22 | R1 checks the generated build with the same compatibility rules as R2 (confirms the Prompt 21 assumption). |
| R-D23 | Invalid LLM answer → one retry with the validation errors as feedback; second answer also invalid → 409 `CompatibilityConflict`. |
| R-D24 | LLM technical failure or timeout (on either call) → 503 `ServiceUnavailable`. |

**Summary of changes:**

- `specs/api-endpoints.md`: R1 note lists what makes an LLM answer valid (product IDs exist and are in stock, exactly one product per component type, R2 compatibility rules, Catalog total price within budget), the single retry with feedback, 409 after the second invalid answer, and 503 on technical failure/timeout. This replaces the earlier "no valid build can be made" wording.
- `specs/domain-model.md`: section 5.5 rules heading is now "Compatibility rules (R1, R2)"; R1 uses the same rules.
- `specs/architecture.md`: Decision 12 adds the compatibility re-check and the retry-once rule.
- No implementation changes.

**Still open:** LLM provider/model (depends on the API key); Recommendation domain-model diagram update; Catalog demo data with canonical categories, spec names and values; FR-19 "agentic AI" tag kept on purpose for now.

**Files created/modified:**

- `specs/api-endpoints.md`
- `specs/domain-model.md`
- `specs/architecture.md`
- `PROMPT_HISTORY.md`
