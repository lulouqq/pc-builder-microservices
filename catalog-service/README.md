# catalog-service

Responsibilities: products, categories, specifications, prices, product reviews.

Port: 8081

## Status

| Story | Status |
|---|---|
| C1 Browse and search products | Implemented |
| C2 View product details | Implemented against a provisional Inventory contract; `ProductViewed` is not published yet |
| C3 View trending products | Calculation logic only; no Kafka, no endpoint (waiting for team decisions) |
| C4 Submit and view product reviews | Implemented |
| C5 Manage products and categories | Implemented; endpoints are not protected yet |

Admin authorisation for the C5 endpoints is an open architecture decision, deferred until Account Service is implemented (see `specs/architecture.md`, decision 11).

## C2: integration with Inventory Service

`GET /products/{productId}` returns the product with a `stockStatus`, obtained from Inventory Service `GET /inventory/{productId}`.

| `stockStatus` | Meaning |
|---|---|
| `IN_STOCK` | Inventory confirmed that `quantity − reservedQuantity` is greater than 0 |
| `OUT_OF_STOCK` | Inventory confirmed that `quantity − reservedQuantity` is 0 or less |
| `UNKNOWN` | No stock information: Inventory Service is down, timed out, returned an error (including 404, no record for the product), or its response did not contain the expected fields |

The service layer depends on the `InventoryClient` interface; `RestInventoryClient` is the only class that knows the Inventory contract. Settings (`application.yml`): `inventory-service.base-url` (default `http://localhost:8082/api`) and `inventory-service.timeout` (default 2 seconds, for connecting and for reading).

**Assumptions to confirm with the Inventory Service team** (Inventory Service is not implemented yet):

1. `InventoryResponse` contains the integer fields `quantity` and `reservedQuantity`. The specs name `InventoryResponse` but do not define its fields; these two come from the `InventoryItem` domain model and are provisional.
2. Available stock is `quantity − reservedQuantity`.
3. A product without an inventory record returns 404; Catalog shows `UNKNOWN` for it.
4. When Inventory Service is unavailable, Catalog still returns the product with `UNKNOWN` instead of failing with 503.
5. `GET /inventory/{productId}` can be called without authentication.

If the agreed contract differs, only `RestInventoryClient` (and `RestInventoryClientTests`) needs to change.

## C3: trending products — proposal for team approval

Nothing below is agreed yet, and none of it is in `specs/`.

**Decisions needed**

1. Which activity counts: product views only (`ProductViewed`), or also reviews and orders.
2. The time window that counts as "recent".
3. How many products the trending list returns.
4. How products with the same count are ordered.
5. Whether repeated views by the same customer count once (this would need a user or session id in the event; the view endpoint has none today).
6. The `ProductViewed` Kafka schema, topic name, message key and format.
7. The Kafka approach for the whole project: a plain consumer with in-service calculation, or Kafka Streams windowing (for example through Spring Cloud Stream).
8. Whether the trending list must survive a restart, and whether removed products leave it immediately.

**Proposed event schema (`ProductViewed`, version 1)**

```json
{
  "eventId": "uuid",
  "eventType": "ProductViewed",
  "version": 1,
  "productId": "string",
  "occurredAt": "2026-10-10T03:15:00Z"
}
```

Field names follow the event fields in `specs/event-sourcing.md`. Proposed topic `catalog.product-viewed`, keyed by `productId`, JSON. Published by Catalog Service when product details are viewed (C2) and consumed by Catalog Service itself (Architecture Decision 9).

**Proposed calculation**

- Count `ProductViewed` events per product in a sliding window that ends now.
- Rank by count, highest first; products with the same count are ordered by their most recent view.
- Return the top N.
- Proposed defaults: window 24 hours, N = 10, both configurable.

**What exists in code**

- `TrendingProductsCalculator` (domain) implements the proposed calculation. The window and N are parameters; no values are fixed in code.
- `ProductViewed` (domain event) is a provisional record with `productId` and `occurredAt`.
- `TrendingProductsCalculatorTests` exercises the calculation with sample events.

**Not done, waiting for the decisions above:** Kafka dependencies and configuration, publishing `ProductViewed` from `GET /products/{productId}`, consuming it, and the `GET /products/trending` endpoint.
