# recommendation-service

Responsibilities: AI-generated PC builds (R1), component compatibility checking (R2), viewing a recommended build (R3) and previous recommendations (R4).

Port: 8085, base path `/api`.

## Status

| Story | Endpoint | Access | Status |
|---|---|---|---|
| R1 Generate recommended PC build | `POST /recommendations` | Customer (owner = body `userId`) | Implemented |
| R2 Validate component compatibility | `POST /compatibility/validate` | Public | Implemented |
| R3 View a recommended build | `GET /recommendations/{recommendationId}` | Customer (owner = stored `userId`) | Implemented (Order Service side of R3 not implemented yet) |
| R4 View previous recommendations | `GET /users/{userId}/recommendations` | Customer (owner = `{userId}`) | Implemented |

Contracts: `specs/api-endpoints.md` ("Recommendation Service DTOs"), rules: `specs/domain-model.md` 5.5, flow: `specs/architecture.md` decision 12.

## Running

```bash
export JWT_SECRET=...          # required, same value as every other service (at least 32 bytes)
export LLM_API_KEY=...         # optional for startup; R1 returns 503 until it and LLM_MODEL_NAME are set
export LLM_MODEL_NAME=...      # the model offered by the provider of the key
export LLM_BASE_URL=...        # optional, default https://api.openai.com/v1
./mvnw spring-boot:run -pl recommendation-service
```

| Property | Environment variable | Default | Meaning |
|---|---|---|---|
| `security.jwt.secret` | `JWT_SECRET` | none (required) | HS256 secret shared with Account Service |
| `recommendation.llm.base-url` | `LLM_BASE_URL` | `https://api.openai.com/v1` | Any OpenAI-compatible chat API (OpenAI, or e.g. Gemini's OpenAI-compatible endpoint) |
| `recommendation.llm.api-key` | `LLM_API_KEY` | empty | API key; no key → no LLM, R1 answers 503 |
| `recommendation.llm.model-name` | `LLM_MODEL_NAME` | empty | Model name; empty → no LLM, R1 answers 503 |
| `recommendation.llm.timeout` | `LLM_TIMEOUT` | `60s` | Timeout of one LLM call |
| `account-service.base-url`, `catalog-service.base-url`, `inventory-service.base-url` | – | `http://localhost:8084/api`, `:8081/api`, `:8082/api` | Downstream services (timeout 2 s each) |

H2 console: http://localhost:8085/api/h2-console (JDBC URL `jdbc:h2:mem:recommendationdb`, user `sa`, no password).

## Design

Layers (package `com.csci318.recommendationservice`):

- `domain` – aggregate roots `BuildRequest` (what the customer asked) and `PCBuild` (what the AI built; owns its `BuildComponent`s and its `Recommendation`), `ComponentType` (the seven canonical types), the `CompatibilityChecker` domain service with its `CompatibilityRule`s, and the internal domain events `BuildRequested`, `RecommendationGenerated`, `CompatibilityChecked` (Spring application events, no Kafka).
- `service` – `RecommendationService` (R1, R3, R4), `CompatibilityService` (R2), `BuildProposalValidator`, and the ports `AccountClient`, `CatalogClient`, `InventoryClient`, `BuildAdvisor` (LLM).
- `infrastructure` – JPA repositories (H2), REST clients, the LangChain4j adapter (`LangChain4jBuildAdvisor`, `LlmConfig`), JWT and security configuration.
- `presentation` – controllers, DTOs, error handling (`{code, message}` bodies, as in Account Service).

### R1 flow

1. Owner check (body `userId` = token subject, else 403) and validation (`budget` > 0, `purpose` not blank, else 400).
2. Saved preference from Account Service `GET /users/{userId}/preferences` (customer token forwarded).
3. Products from Catalog Service `GET /categories` + `GET /products`, mapped locally; only products in a canonical category.
4. Stock from Inventory Service `GET /inventory/{productId}` for each of them; only in-stock products are kept (404 = not available).
5. If some component type has no in-stock product, or the cheapest possible build is over the budget → 409 without calling the LLM.
6. LangChain4j sends the context (budget, purpose, request preference and saved preference as separate lines, available products) to the LLM, which answers with JSON `{productIds, description, reasoning}`. No tool calling.
7. Java re-checks the answer: known and in-stock product IDs, exactly one per component type, R2 compatibility rules, total price (from Catalog) within budget, description and reasoning present.
8. Invalid answer → asked once more with the problems; still invalid → 409 `CompatibilityConflict`.
9. Valid → `BuildRequest`, `PCBuild`, `BuildComponent`s and `Recommendation` stored in one transaction, events published, 201.

Errors: Account Service's 401, 403 and 404 are passed on as `AuthenticationRequired` (401), `AccessDenied` (403) and `ResourceNotFound` (404). A timeout, network failure or 5xx from Account, Catalog or Inventory, or an LLM failure/timeout → 503 `ServiceUnavailable`. Nothing is stored when R1 fails.

### R2 rules

CPU ↔ Motherboard `Socket`, RAM ↔ Motherboard `Memory Type`, Motherboard ↔ Case `Form Factor`. A rule is skipped when one of its types is not selected; a missing specification is a conflict; values are compared trimmed and ignoring case. Always 200 (`compatible` true/false with `conflicts`); 400 for an empty list, two products of one type or a non-canonical product; 404 for an unknown product; 503 when Catalog is unavailable. R2 uses no LLM and stores nothing.

## Assumptions to confirm

1. **Inventory response without stock fields:** the contract is `InventoryResponse {productId, quantity, reservedQuantity}` (specs/api-endpoints.md), available = `quantity − reservedQuantity`; a response without `quantity`/`reservedQuantity` is treated as 503.
2. **Category names** are matched to the canonical types ignoring surrounding whitespace and case. If a product has the same specification name twice, the first value is used.
3. **Domain event payloads** (not defined in the specs): `BuildRequested(buildRequestId, userId, budget, purpose, occurredAt)`, `RecommendationGenerated(recommendationId, buildId, buildRequestId, userId, totalPrice, occurredAt)`, `CompatibilityChecked(productIds, compatible, conflictCount, occurredAt)`. `BuildRequested` is published after the request is stored, i.e. only for successful R1 calls.
4. **LLM provider:** LangChain4j `langchain4j-open-ai` 1.4.0 (OpenAI-compatible API, provider chosen by `LLM_BASE_URL`). Technical failures are not retried (`maxRetries(0)`). This module sets Jackson to 2.19.2, the version LangChain4j 1.4.0 is built with.

## Tests

`./mvnw test -pl recommendation-service` — no API key and no other service needed: the LangChain4j `ChatModel` and the Account/Catalog/Inventory clients are mocks in the API tests, and the REST clients are tested against local stub HTTP servers.
