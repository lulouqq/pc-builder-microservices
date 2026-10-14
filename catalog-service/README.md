# catalog-service

Responsibilities: products, categories, specifications, prices, product reviews.

Port: 8081

## Status

| Story | Status |
|---|---|
| C1 Browse and search products | Implemented |
| C2 View product details | Implemented; a successful lookup publishes `ProductViewed` (C3) |
| C3 View trending products | Implemented (`GET /products/trending`, Kafka topic `catalog-events`) |
| C4 Submit and view product reviews | Implemented; submitting a review needs the reviewer's own token |
| C5 Manage products and categories | Implemented; ADMIN token required |

## Authentication and authorisation

Design: `specs/api-endpoints.md` "Authentication and Authorisation" and `specs/architecture.md` decision 11. The service validates the JWT issued by Account Service itself (HS256, shared secret, stateless OAuth2 resource server); it does not call Account Service.

| Access | Endpoints |
|---|---|
| Public | `GET /products`, `GET /products/{productId}`, `GET /products/{productId}/reviews`, `GET /categories` |
| Customer (owner) | `POST /products/{productId}/reviews`: the token's `sub` must equal the body's `userId` (403 otherwise, checked before the product is looked up; `ADMIN` does not bypass it) |
| Admin | `POST /products`, `PUT`/`DELETE /products/{productId}`, `POST /categories`, `PUT`/`DELETE /categories/{categoryId}` |

- A missing, malformed, expired or wrongly signed token on a protected endpoint returns `401 AuthenticationRequired`; a `CUSTOMER` token on an admin endpoint, or another user's id in a review, returns `403 AccessDenied`.
- Every error body is `{"code", "message"}` (API error response contract).
- `JWT_SECRET` (`security.jwt.secret`) has no default and must be set before starting the service, with the same value (at least 32 bytes) as in Account Service. Tests use the `test` profile (`src/test/resources/application-test.yml`).

```bash
JWT_SECRET='<same secret as Account Service>' ./mvnw spring-boot:run -pl catalog-service
```

## C2: integration with Inventory Service

`GET /products/{productId}` returns the product with a `stockStatus`, obtained from Inventory Service `GET /inventory/{productId}`.

| `stockStatus` | Meaning |
|---|---|
| `IN_STOCK` | Inventory confirmed that `quantity − reservedQuantity` is greater than 0 |
| `OUT_OF_STOCK` | Inventory confirmed that `quantity − reservedQuantity` is 0 or less |
| `UNKNOWN` | No stock information: Inventory Service is down, timed out, returned an error (including 404, no record for the product), or its response did not contain the expected fields |

The service layer depends on the `InventoryClient` interface; `RestInventoryClient` is the only class that knows the Inventory contract. Settings (`application.yml`): `inventory-service.base-url` (default `http://localhost:8082/api`) and `inventory-service.timeout` (default 2 seconds, for connecting and for reading).

**Inventory contract** (`specs/api-endpoints.md`, Inventory Service DTOs; Architecture Decisions 8 and 13):

1. `InventoryResponse` has the fields `productId`, `quantity` and `reservedQuantity`; Catalog uses the two integer fields `quantity` and `reservedQuantity`.
2. Available stock is `quantity − reservedQuantity`.
3. A product without an inventory record returns 404; Catalog shows `UNKNOWN` for it.
4. When Inventory Service is unavailable, Catalog still returns the product with `UNKNOWN` instead of failing with 503 (NFR-03).
5. `GET /inventory/{productId}` is public, so Catalog calls it without a token.

## C3: trending products

Design: `specs/architecture.md` decision 16 and Kafka Event Contracts; `specs/api-endpoints.md` (Catalog Service).

- **Input: product views only.** A successful `GET /products/{productId}` (C2) raises `ProductViewed`; an unknown product (404), `GET /products` and `GET /products/trending` do not. Every view counts.
- **Kafka.** `ProductViewedKafkaPublisher` sends it to the topic `catalog-events` (key `productId`) after the request's transaction, as JSON with `eventId`, `eventType`, `version` (1), `occurredAt` (ISO-8601 UTC) and `productId`. A failed send is logged and does not fail C2. `ProductViewedKafkaListener` (consumer group `catalog-service`) consumes it and stores the view; a message that is not usable is skipped, and a repeated `eventId` is stored once.
- **Kafka disabled** (`KAFKA_ENABLED=false`): `InProcessProductViewRecorder` stores the view directly, so the trending list still works without a broker.
- **Storage.** Views are stored in H2 (`product_view`: `eventId`, `productId`, `occurredAt`).
- **`GET /products/trending`** (public): the top `catalog.trending.size` products (default 10) by number of views in the last `catalog.trending.window` (default 24 hours), ranked by `TrendingProductsCalculator`: most viewed first, then most recent view, then product id. Removed products are left out, so the list can be shorter or empty. The response is the unchanged `ProductResponse` with `stockStatus` = `null`.
- One Catalog Service instance is assumed: the trending list is kept per instance.

| Setting | Environment variable | Default |
|---|---|---|
| `catalog.trending.window` | `TRENDING_WINDOW` | `24h` |
| `catalog.trending.size` | `TRENDING_SIZE` | `10` |
| `spring.kafka.bootstrap-servers` | `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| `messaging.kafka.enabled` | `KAFKA_ENABLED` | `true` |

Tests: `TrendingProductsCalculatorTests` (ranking), `TrendingProductsTests` (endpoint and in-process recording), `ProductViewedKafkaMessagingTests` (message format, send failures, listener parsing) and `CatalogKafkaIntegrationTests` (embedded Kafka: C2 → `catalog-events` → trending list, repeated messages).
