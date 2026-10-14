# Prompt History – recommendation-service

This file records AI-assisted development prompts that relate **only to `recommendation-service`**.
Project-wide prompts (setup, architecture, specifications, cross-service changes) are recorded in the root [`PROMPT_HISTORY.md`](../PROMPT_HISTORY.md).

For every substantial AI-assisted change to this service, append an entry with: date, goal, exact user prompt, short summary of changes, and files created/modified.

---

## Prompt 1

**Date:** 2026-10-14

**Goal:** Pre-implementation review of recommendation-service: list stories/endpoints, dependencies on Account/Catalog/Inventory, where LangChain4j is used, and the open questions to decide before implementation. Review only — no code changes.

**Exact user prompt:**

````text
Read CLAUDE.md, the relevant Recommendation specs, recommendation-service/README.md, and recommendation-service/PROMPT_HISTORY.md. Look at the current service state. List all Recommendation stories/endpoints, show all dependencies on Account/Catalog/Inventory, simply explain where LangChain4j is used, and list all uncertainties and decisions that need to be made before implementation. Don't change any code. Only add this prompt and the review to recommendation-service/PROMPT_HISTORY.md
````

### Review

**Current state**

- `recommendation-service/` contains only `README.md` and this file: no `pom.xml`, no code, not listed in the root `pom.xml` `<modules>`.
- Catalog Service: C1, C2–C5 implemented (`GET /products` with `categoryId`/`brand`/`specName`+`specValue` filters, `GET /products/{productId}`, `GET /categories`). No Spring Security yet. No seed data.
- Account Service: A1–A3 implemented, including `GET /users/{userId}/preferences` (owner-only JWT) and the JWT resource-server setup (`SecurityConfig`, `JwtConfig`, env var `JWT_SECRET`) that can be reused.
- Inventory Service and Order Service: placeholders only (no code). `InventoryResponse` fields are not in the specs; Catalog's `RestInventoryClient` assumes `quantity` and `reservedQuantity`.

**Stories and endpoints** (base `http://localhost:8085/api`)

| Story | FR | Endpoint | Request → Response | Access | Domain classes | Domain events |
|---|---|---|---|---|---|---|
| R1 Generate Recommended PC Build | FR-17, FR-18 | `POST /recommendations` | `BuildRecommendationRequest` → 201 `RecommendationResponse` | Customer (owner = `userId` in body) | BuildRequest, PCBuild, BuildComponent, Recommendation | BuildRequested, RecommendationGenerated |
| R2 Validate Component Compatibility | FR-19 | `POST /compatibility/validate` | `CompatibilityRequest` → 200 `CompatibilityResponse` | Public | PCBuild, BuildComponent, Product, Specification | CompatibilityChecked |
| R3 Add Recommended Build to Cart | FR-20 | `GET /recommendations/{recommendationId}` (this service); `POST /orders/from-recommendation/{recommendationId}` (Order Service) | → 200 `RecommendationResponse` | Customer (owner = stored `userId`; unknown id 404, other user 403) | PCBuild, BuildComponent (+ Order, OrderItem) | None |
| R4 View Previous Recommendations | FR-21 | `GET /users/{userId}/recommendations` | → 200 `Array<RecommendationResponse>` | Customer (owner = `{userId}`, 403 before lookup) | BuildRequest, PCBuild, Recommendation | None |

Error codes relevant to this service: `ValidationFailed` 400, `AuthenticationRequired` 401, `AccessDenied` 403, `ResourceNotFound` 404, `CompatibilityConflict` 409, `InternalServiceError` 500, `ServiceUnavailable` 503.

Domain model (5.1–5.4): BuildRequest {id, userId, budget, purpose, preferences?}; PCBuild {id, requestId, totalPrice, description}; BuildComponent {id, productId, componentType, price}; Recommendation {id, buildId, reasoning}. Associations: BuildRequest 1—1 PCBuild (composition in diagram), PCBuild 1—1..* BuildComponent, PCBuild 1—0..1 Recommendation, User 1—0..* BuildRequest.

**Dependencies**

| Direction | Call | Auth | Used for | Status of the other side |
|---|---|---|---|---|
| Recommendation → Account | `GET /users/{userId}/preferences` → `PreferenceResponse {userId, preference}` | Forward customer's `Authorization` header | R1: saved free-text preference (A3 scenario step 3) | Implemented |
| Recommendation → Catalog | `GET /products` (filters), `GET /categories`, `GET /products/{productId}` → `ProductResponse {id, name, price, brand, categoryId, specifications[{name,value}], stockStatus}` | None (public) | R1: candidate products and prices; R2: component specifications | Implemented |
| Recommendation → Inventory | `GET /inventory/{productId}` → `InventoryResponse` (fields undefined) | None (public) | R1: live stock check | Not implemented |
| Order → Recommendation | `GET /recommendations/{recommendationId}` | Forwarded customer token | R3: build components for a DRAFT order | Order Service not implemented |
| Recommendation → LLM provider | LLM API ("OpenAI / Gemini etc") | API key | R1, R2 | External |

No Kafka flows involve Recommendation Service; its domain events are not listed in the architecture Kafka flows.

**Where LangChain4j is used (simply)**

LangChain4j is the bridge between the Java service and the LLM. In R1 it runs an "AI agent": the service gives the model the request (budget, purpose, preferences) plus access to catalog products and stock (either as tools the model can call, or as data put in the prompt), and the model picks one product per component type and writes an explanation; Java code then checks the answer and saves it as BuildRequest/PCBuild/BuildComponents/Recommendation. In R2 the model gets the selected components' specifications and returns the list of conflicts. LangChain4j would be used only in the service/infrastructure layer (an AI service interface + model configuration); controllers, domain classes, persistence and other-service calls are plain Spring code. R3 and R4 do not use the LLM.

**Uncertainties and decisions needed before implementation**

API contract (not defined in specs):

1. Fields of `BuildRecommendationRequest` — likely `userId, budget, purpose, preferences?` (owner check uses body `userId`).
2. Fields of `RecommendationResponse` — must cover R1, R3 (components with productId and price, for Order Service) and R4 ("previous build requests" → budget, purpose, preferences too). Proposed: `id, userId, budget, purpose, preferences, totalPrice, description, reasoning, components[{productId, componentType, price}]`; maybe product `name`.
3. Fields of `CompatibilityRequest` (list of `productId`s? with `componentType`?) and `CompatibilityResponse` (e.g. `compatible: boolean, conflicts[{productIds, reason}]`).
4. `CompatibilityConflict` (409) vs R2's `200 OK (CompatibilityResponse)`: does R2 return 200 with conflicts listed, or 409 when conflicts exist? Or is 409 only for R1 when the generated build is incompatible?
5. Which id is `{recommendationId}` in R3/R4 — the Recommendation id (spec), with the build reached via `buildId`. Recommendation is 0..1 per PCBuild: can a PCBuild exist without a Recommendation (e.g. generation failed)?

Domain / persistence:

6. Aggregate boundaries: the table marks both BuildRequest and PCBuild as aggregate roots, but the diagram shows BuildRequest ◆ PCBuild (composition). Choose one: (a) BuildRequest root containing PCBuild → BuildComponents and Recommendation, or (b) separate roots linked by ids.
7. R2 maps to PCBuild/BuildComponent but is public and has no BuildRequest (`PCBuild.requestId` is required). Proposed: R2 persists nothing (stateless check).
8. BuildComponent has no `quantity` → each component counts as quantity 1 (affects Order R3). Allowed `componentType` values and required set (CPU, motherboard, RAM, GPU, storage, PSU, case, cooler?) — is GPU optional for work builds?
9. How `componentType` maps to Catalog data: Product has only `categoryId`; use the Category name (requires agreed category names like "CPU", "GPU").
10. No `createdAt` on BuildRequest/Recommendation → R4 ordering undefined (adding it is a spec change).
11. `BuildRequested`, `RecommendationGenerated`, `CompatibilityChecked`: internal domain events only (not Kafka), payload fields undefined.
12. Store `preferences`/`reasoning`/`description` as large text (as Account D3)?

Preferences (A3 → R1):

13. How request `preferences` and saved Account preference combine: request overrides, both concatenated, or saved used only when request is empty? What is stored in `BuildRequest.preferences`?
14. Account unavailable (NFR-03): continue without saved preference, or fail with 503 `ServiceUnavailable`?

Catalog / Inventory data:

15. Spec naming conventions for compatibility (e.g. `socket`, `memoryType`, `formFactor`, `wattage`, `tdp`) — Catalog specifications are free-form name/value pairs and there is no seed data. Who creates demo products and with which spec names?
16. `GET /products/{productId}` publishes `ProductViewed` (trending). Calling it from R1/R2 would distort trending; proposed: use `GET /products` (list) and filter locally.
17. `InventoryResponse` fields undefined and Inventory Service not implemented; same `quantity − reservedQuantity` assumption as Catalog? No batch endpoint → one call per candidate product.
18. Stock rules: only in-stock products? Inventory unavailable / no record (`UNKNOWN`) → exclude, include with a warning, or 503?
19. Should R2 also check stock, or only compatibility?

AI / LangChain4j:

20. LLM provider and model (OpenAI, Gemini, other); API key via environment variable (like `JWT_SECRET`); LangChain4j version compatible with Spring Boot 3.3.2.
21. Agent style: LangChain4j `@Tool` methods the model calls (closer to "agentic AI" in FR-18/FR-19) vs. pre-fetching products/stock and putting them in the prompt (simpler, fewer calls).
22. R2: AI-only compatibility (as the story says) vs deterministic rule checks + AI explanation (more reliable and testable).
23. Validation of AI output: reject/retry if unknown productIds, wrong component types, out-of-stock items, or total > budget; prices and `totalPrice` always taken from Catalog, not the LLM. Number of retries; what error if no valid build is possible (budget too low)?
24. NFR-09 "acceptable response time": timeout value, and error when the LLM fails/times out (500 or 503).
25. Tests: mock the LLM (fake `ChatLanguageModel`/AI service) and the Account/Catalog/Inventory clients so `./mvnw test` needs no API key or running services.

Security / integration:

26. Reuse the Account Service JWT resource-server setup (same `security.jwt.secret`); add the `oauth2-resource-server` dependency.
27. Order Service (R3) contract: Order uses `price` from the recommendation or the current Catalog price as `unitPrice`? (Order-side decision, but `RecommendationResponse` must support it.)

**Summary of changes:**

- Review only; no code, configuration or spec changes.
- Noted spec gaps/conflicts to resolve (not changed): undefined R1–R4 DTOs; `CompatibilityConflict` 409 vs R2 200; BuildRequest/PCBuild aggregate-root vs composition; R2 domain mapping vs public stateless endpoint; undefined `InventoryResponse`; `ProductViewed` side effect of `GET /products/{productId}`.

**Files created/modified:**

- `recommendation-service/PROMPT_HISTORY.md`

---

## Prompt 2

**Date:** 2026-10-14

**Goal:** Record the team's answers to the Prompt 1 open questions, and inspect the current Catalog API/DTOs (categories, specifications, product lists) before implementing Recommendation. No implementation.

**Exact user prompt:**

````text
DTOs. R1 request: userId, budget, purpose, preferences optional. Recommendation response: recommendationId, buildId, userId, totalPrice, description/reasoning, components[] с productId, componentType, price. R2 request: list productIds. Response: compatible + conflicts[]. R3 returns same full Recommendation response. R4 - list responses. 

R2 and 409. Validation is always 200: compatible build - compatible:true, incompatible - compatible:false + conflicts. 409 CompatibilityConflict for /compatibility/validate not using. 

Domain model. Leave BuildRequest and PCBuild aggregate roots as written in domain model. BuildRequest = what user asked, PCBuild = what the AI built. Old diagram consider out of date will fix it later. 

LangChain4j. Right now not implement agent/tool-calling. Java receives Account preferences + Catalog products + Inventory stock then formates the context, LangChain4j sends it to LLM, LLM returns selected product IDs + reasoning. Then Java rechecks: are there real IDs, prices, budget, stock.

LLM provider. API key and model through env/config, tests are without real API - mock LLM. Which exact provider.model to get for real launch, depends on future API key. 

Preferences. Saved Account preference + preference from current R1 request are sent to the model separately. Current request is more precise, so in case of conflict, the current request takes priority. If Account service not available - 503 ServiceUnavailable. 

Inventory. In recommendation only in-stock products can be recommended. Inventory unavailable - 503. 

Events/security. BuildRequested, RecommendationGenerated, CompatibilityChecked - internal events only. JWT is checked the same way: R1/R3/R4 protected, R2 public. Recommendation/Order integration later will provide customer Bearer token.

Before implementing Recommendation, inspect the current Catalog API and DTOs and tell me how categories, specifications and product lists actually work. Do not implement Recommendation yet.
````

### Decisions (answers to Prompt 1)

| # | Topic | Decision |
|---|---|---|
| RD1 | DTOs | `BuildRecommendationRequest` = `userId, budget, purpose, preferences?`. `RecommendationResponse` = `recommendationId, buildId, userId, totalPrice, description/reasoning, components[{productId, componentType, price}]`. `CompatibilityRequest` = list of `productIds`. `CompatibilityResponse` = `compatible, conflicts[]`. R3 returns the same full `RecommendationResponse`; R4 returns a list of them. |
| RD2 | R2 status codes | `POST /compatibility/validate` always returns 200: `compatible: true`, or `compatible: false` + `conflicts`. `CompatibilityConflict` (409) is not used by this endpoint. |
| RD3 | Domain model | BuildRequest and PCBuild stay separate aggregate roots as in the domain-model table (BuildRequest = what the user asked, PCBuild = what the AI built). The diagram's composition is out of date; to be fixed later. |
| RD4 | LangChain4j | No agent/tool-calling for now. Java collects Account preference + Catalog products + Inventory stock, formats the context; LangChain4j sends it to the LLM; the LLM returns selected product IDs + reasoning; Java re-checks IDs, prices, budget and stock. |
| RD5 | LLM provider | API key and model from env/config; tests use a mocked LLM (no real API). Exact provider/model decided later, depending on the available API key. |
| RD6 | Preferences | Saved Account preference and the R1 request preference are sent to the model as separate inputs; the current request wins on conflict. Account unavailable → 503 `ServiceUnavailable`. |
| RD7 | Inventory | Only in-stock products can be recommended. Inventory unavailable → 503 `ServiceUnavailable`. |
| RD8 | Events / security | `BuildRequested`, `RecommendationGenerated`, `CompatibilityChecked` are internal domain events only (no Kafka). JWT validated as in Account Service: R1/R3/R4 protected (owner), R2 public. Order Service will forward the customer Bearer token for R3. |

### Catalog API inspection (current code, `catalog-service`)

**Categories**

- `Category {id, name}`; `id` is a server-generated UUID. `GET /categories` → `[CategoryResponse {id, name}]`, sorted by name.
- Names are free text: no fixed list, no uniqueness constraint, can be renamed (C5); no seed data. Nothing in Catalog says which category is "CPU", "GPU", etc.
- A product references its category only by `categoryId`; `ProductResponse` has no category name.

**Specifications**

- Value object `Specification {name, value}`, both non-blank strings, stored as an `@ElementCollection` of the product (table `product_specification`). Every product needs at least one.
- Free-form: no fixed spec names, no units, no numeric types (e.g. `"Wattage" = "750W"` is just text); duplicate names on one product are not prevented.
- Search matches one spec by exact `name` + `value`, case-insensitive; `specName` without `specValue` (or the reverse) → 400 `ValidationFailed`.

**Product list — `GET /products`**

- Optional filters `categoryId` (exact), `brand` (exact, case-insensitive), `specName`+`specValue`; combined with AND; sorted by product name.
- Returns every matching product: no pagination, no price-range filter, no "by ids" filter. Unknown `categoryId` → empty list, not an error.
- `ProductResponse {id, name, price (Float), brand, categoryId, specifications[{name, value}], stockStatus}`; `stockStatus` is `null` in lists.

**Product details — `GET /products/{productId}`**

- 404 `ResourceNotFound` for an unknown id. Calls Inventory and sets `stockStatus` `IN_STOCK`/`OUT_OF_STOCK`/`UNKNOWN`; `UNKNOWN` covers both "Inventory down" and "no inventory record".
- `ProductViewed` is not published yet (C3 Kafka part pending), but the spec says this endpoint will publish it.

**Other:** Catalog has no Spring Security yet (all endpoints open); errors use `ErrorResponse {error, message}`; Inventory contract in Catalog is provisional (`quantity`, `reservedQuantity`).

**Implications for Recommendation (proposals, not decided)**

1. `componentType` = Catalog category name. Needs an agreed fixed set of category names (e.g. CPU, Motherboard, RAM, GPU, Storage, PSU, Case) and demo products created with them; Recommendation maps names → ids via `GET /categories`.
2. Compatibility needs agreed spec names/values (e.g. `Socket`, `Memory Type`, `Form Factor`, `Wattage`), since Catalog specs are free text.
3. Load candidates with `GET /products` (one call, optionally per category), not `GET /products/{id}`: avoids future `ProductViewed` side effects and per-product Inventory calls; R2 can also resolve its `productIds` from this list.
4. Do not use Catalog `stockStatus` (null in lists; `UNKNOWN` mixes "down" and "no record"); call Inventory `GET /inventory/{productId}` directly so RD7's 503 rule can be applied.
5. Still open: Catalog unavailable → 503 (consistent with RD6/RD7)? R2 with an unknown productId → 404 or a conflict entry? R2 with an empty list → 400? Inventory "no record" (404) → treat as out of stock?

**Spec/decision gaps noticed (not changed)**

- RD1 "description/reasoning": domain has both `PCBuild.description` and `Recommendation.reasoning` — return both fields or one?
- RD1 `RecommendationResponse` has no `budget`/`purpose`/`preferences`, but R4 acceptance rule 1 ("view their previous build requests") needs them. Smallest fix: add those fields to the response.
- RD1–RD2 DTOs are not yet in `specs/api-endpoints.md` (A3 precedent: add a Recommendation DTO table).

**Summary of changes:**

- Recorded decisions RD1–RD8 and the Catalog inspection. No code, configuration or spec changes.

**Files created/modified:**

- `recommendation-service/PROMPT_HISTORY.md`
