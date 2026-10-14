# API Endpoints

> Source: `source-docs/API Endpoints-2.docx`

## Service Port Mapping and Base Path

| Service | Port | Base URL |
|---|---|---|
| Catalog Service | 8081 | `http://localhost:8081/api` |
| Inventory Service | 8082 | `http://localhost:8082/api` |
| Order Service | 8083 | `http://localhost:8083/api` |
| Account Service | 8084 | `http://localhost:8084/api` |
| Recommendation Service | 8085 | `http://localhost:8085/api` |

Endpoint paths below are relative to the service base URL.

### Catalog Service (http://localhost:8081/api)

| Feature | Method | Endpoint Path | Request Body | Response (Success) | Description |
|---|---|---|---|---|---|
| C1 | GET | /products | None | 200 OK (Array\<ProductResponse\>) | Browse and search products. Query parameters may be used for category, brand, and specification filters |
| C1 | GET | /categories | None | 200 OK (Array\<CategoryResponse\>) | List product categories, used to browse products by category |
| C2 | GET | /products/{productId} | None | 200 OK (ProductResponse) | View detailed information for a specific product |
| C3 | GET | /products/trending | None | 200 OK (Array\<ProductResponse\>) | View products currently trending based on recent customer activity |
| C4 | GET | /products/{productId}/reviews | None | 200 OK (Array\<ReviewResponse\>) | View reviews and ratings for a product |
| C4 | POST | /products/{productId}/reviews | ReviewCreateRequest | 201 Created (ReviewResponse) | Submit a review and rating for a product |
| C5 | POST | /products | ProductCreateRequest | 201 Created (ProductResponse) | Create a new product |
| C5 | PUT | /products/{productId} | ProductUpdateRequest | 200 OK (ProductResponse) | Update an existing product |
| C5 | DELETE | /products/{productId} | None | 204 No Content | Remove a product |
| C5 | POST | /categories | CategoryCreateRequest | 201 Created (CategoryResponse) | Create a new product category |
| C5 | PUT | /categories/{categoryId} | CategoryUpdateRequest | 200 OK (CategoryResponse) | Update a product category |
| C5 | DELETE | /categories/{categoryId} | None | 204 No Content | Remove a product category |

#### Catalog Service Query Parameters

All `GET /products` (C1) query parameters are optional and are combined with AND.

| Parameter | Description |
|---|---|
| categoryId | Return only products in this category |
| brand | Return only products of this brand |
| specName, specValue | Return only products that have a specification with this name and value; used together |

#### Catalog Service DTOs

Field names and types follow the [Catalog Service Domain Model](domain-model.md#1-catalog-service-domain-model).

| DTO | Fields |
|---|---|
| ProductCreateRequest | name, price, brand, categoryId, specifications (Array of {name, value}) |
| ProductUpdateRequest | name, price, brand, categoryId, specifications (Array of {name, value}) |
| ProductResponse | id, name, price, brand, categoryId, specifications (Array of {name, value}), stockStatus |
| CategoryCreateRequest | name |
| CategoryUpdateRequest | name |
| CategoryResponse | id, name |
| ReviewCreateRequest | userId, rating (1–5), comment (optional) |
| ReviewResponse | id, productId, userId, rating, comment, createdAt |

> `ProductResponse.stockStatus` (`IN_STOCK` / `OUT_OF_STOCK` / `UNKNOWN`) is set only by `GET /products/{productId}` (C2), from Inventory Service `GET /inventory/{productId}`: `IN_STOCK` when `quantity − reservedQuantity` is greater than 0, otherwise `OUT_OF_STOCK`, and `UNKNOWN` when Inventory Service is unavailable or has no record for the product (NFR-03). It is `null` in list responses (C1, C3) and in C5 responses.
>
> `GET /products/{productId}` also publishes `ProductViewed` to Kafka (topic `catalog-events`) for the trending list (C3), only when the product is found; it does not change the product itself. A failed publish is logged and does not fail the request.
>
> `GET /products/trending` (C3) returns at most 10 products (`catalog.trending.size`), ranked by the number of times their details were viewed in the last 24 hours (`catalog.trending.window`): most viewed first; equal counts are ordered by the most recent view, newest first, then by product id. Every view counts. Removed products are left out, so the list can have fewer than 10 products, and it is empty when nothing was viewed in the window. See [Architecture Decision 16](architecture.md#architecture-decisions).

### Inventory Service (http://localhost:8082/api)

| Feature | Method | Endpoint Path | Request Body | Response (Success) | Description |
|---|---|---|---|---|---|
| I1 | POST | /inventory/{productId}/reservations | StockReservationRequest | 201 Created (StockReservationResponse) | Reserve available stock during checkout |
| I1 | DELETE | /inventory/{productId}/reservations/{reservationId} | None | 204 No Content | Release an existing stock reservation |
| I2 | POST | /inventory/{productId}/notifications | StockNotificationRequest | 201 Created (StockNotificationResponse) | Register a customer to receive stock notifications for a product |
| I2 | GET | /users/{userId}/stock-alerts | None | 200 OK (Array\<StockAlertResponse\>) | Retrieve the customer's stock alerts for subscribed products |
| I3 | PUT | /inventory/{productId} | StockUpdateRequest | 200 OK (InventoryResponse) | Update the stock level of a product |
| I3 | GET | /inventory/{productId} | None | 200 OK (InventoryResponse) | Retrieve current stock information for a product |

> In the normal order flow, stock is reserved and released through Kafka events (`OrderPlaced` / `OrderCancelled`), not through these REST endpoints. The I1 REST endpoints are kept for direct use and testing — see [Architecture Decisions](architecture.md#architecture-decisions).

#### Inventory Service DTOs

Field names and types follow the [Inventory Service Domain Model](domain-model.md#2-inventory-service-domain-model).

| DTO | Fields |
|---|---|
| InventoryResponse | productId, quantity, reservedQuantity |
| StockUpdateRequest | quantity (new absolute stock level, 0 or more), reason (optional) |
| StockReservationRequest | orderId, quantity (1 or more) |
| StockReservationResponse | id, productId, orderId, quantity, status (`ACTIVE` / `RELEASED` / `CONFIRMED`) |
| StockNotificationRequest | userId |
| StockNotificationResponse | id, productId, userId, createdAt |
| StockAlertResponse | id, userId, productId, type (`STOCK_LOW` / `PRODUCT_RESTOCKED`), availableQuantity, message, occurredAt |

> `InventoryResponse` is the contract read by Catalog Service (C2 `stockStatus`) and Recommendation Service (R1) through `GET /inventory/{productId}`: available stock is `quantity − reservedQuantity`, and a product is in stock when it is greater than 0. `GET /inventory/{productId}` returns `ResourceNotFound` (404) when the product has no inventory record.
>
> `PUT /inventory/{productId}` (I3) sets the absolute stock quantity. The first update for a product that has no inventory record creates the record (`InventoryInitialized` with no stock, followed by `StockLevelUpdated`); Inventory Service does not check that the product exists in Catalog Service. `reason` is optional and is recorded in `StockLevelUpdated`. A missing or negative `quantity`, or a `quantity` lower than the reserved quantity, returns `ValidationFailed` (400).
>
> `POST /inventory/{productId}/reservations` (I1) returns `StockUnavailable` (409) when `quantity` is greater than the available stock. `DELETE /inventory/{productId}/reservations/{reservationId}` returns `ReservationConflict` (409) when the reservation does not exist for the product or is not `ACTIVE`. Both return `ResourceNotFound` (404) when the product has no inventory record.
>
> `POST /inventory/{productId}/notifications` (I2) returns `ResourceNotFound` (404) when the product has no inventory record. Subscribing again with the same `userId` returns the existing subscription.
>
> `GET /users/{userId}/stock-alerts` (I2) returns the user's alerts newest first (by `occurredAt`), or an empty array when there are none; Inventory Service does not know which users exist, so it never returns 404. Alerts are created asynchronously from `StockChanged` ([Architecture Decision 17](architecture.md#architecture-decisions)): an alert is available within a few seconds of the stock change, and the client polls this endpoint to receive it (no push delivery). Every subscription of the product gets one alert per `STOCK_LOW` / `PRODUCT_RESTOCKED`; subscriptions stay active. There is no unsubscribe, read/unread state or pagination yet.

### Order Service (http://localhost:8083/api)

| Feature | Method | Endpoint Path | Request Body | Response (Success) | Description |
|---|---|---|---|---|---|
| O1 | POST | /orders | OrderCreateRequest | 201 Created (OrderResponse) | Create a DRAFT order (the customer's cart) |
| O1 | POST | /orders/{orderId}/items | OrderItemCreateRequest | 201 Created (OrderResponse) | Add a product to a DRAFT order (cart) |
| O1 | DELETE | /orders/{orderId}/items/{orderItemId} | None | 204 No Content | Remove a product from a DRAFT order (cart) |
| O1 | POST | /orders/{orderId}/checkout | None | 202 Accepted (OrderResponse) | Check out the order (DRAFT → PLACED) and begin the order / inventory flow; the stock reservation result arrives asynchronously |
| O2 | PUT | /orders/{orderId}/delivery-address | DeliveryAddressRequest | 200 OK (OrderResponse) | Select a saved delivery address for an order |
| O4 | PUT | /orders/{orderId}/payment | PaymentStatusRequest | 200 OK (OrderResponse) | Process and record the payment status of an order |
| O5 | GET | /orders/{orderId} | None | 200 OK (OrderResponse) | Retrieve and track a specific order |
| O5 | GET | /users/{userId}/orders | None | 200 OK (Array\<OrderResponse\>) | Retrieve a customer's order history |
| O6 | POST | /orders/{orderId}/cancel | None | 200 OK (OrderResponse) | Cancel an order that has not yet been fulfilled |
| R3 | POST | /orders/from-recommendation/{recommendationId} | None | 201 Created (OrderResponse) | Create a DRAFT order (cart) containing the components of a recommended PC build |

> The cart is an Order in `DRAFT` status; there are no separate cart endpoints. See [Order Lifecycle](domain-model.md#33-order-lifecycle). Item and checkout operations on an order that is not `DRAFT`, payment on an order that is not `PROCESSING`, and cancelling a `COMPLETED` order return `OrderStateViolation` (409).

#### Order Service DTOs

Field names and types follow the [Order Service Domain Model](domain-model.md#3-order-service-domain-model).

| DTO | Fields |
|---|---|
| OrderCreateRequest | userId |
| OrderItemCreateRequest | productId, quantity (1 or more) |
| DeliveryAddressRequest | addressId (id of one of the customer's saved addresses in Account Service) |
| PaymentStatusRequest | paymentStatus (`PAID` / `FAILED`) |
| OrderResponse | id, userId, status, totalPrice, createdAt, deliveryAddressId, paymentStatus, paymentDueAt, items (Array of {id, productId, quantity, unitPrice}) |

> **Cart (O1).** A customer can have only one `DRAFT` order: `POST /orders` returns `OrderStateViolation` (409) when the customer already has one. `POST /orders/{orderId}/items` takes `unitPrice` from the product's current price in Catalog Service (read with `GET /products`, not `GET /products/{productId}`); a `productId` that is not in the catalog returns `ResourceNotFound` (404). Adding a product that is already in the cart increases its quantity instead of adding a second item, and updates its `unitPrice` to the current price. Removing an `orderItemId` that is not in the order returns `ResourceNotFound` (404).
>
> **Delivery address (O2).** The address can be selected while the order is `DRAFT`. Order Service reads the customer's saved addresses from Account Service (`GET /users/{userId}/addresses`, forwarding the token); an `addressId` that is not one of them returns `ResourceNotFound` (404).
>
> **Checkout (O1).** Checkout needs at least one item and a selected delivery address; otherwise it returns `OrderStateViolation` (409). It returns 202 Accepted with the order in status `PLACED`: stock is reserved asynchronously, and the result (`StockReserved` or `StockReservationFailed`) moves the order to `PROCESSING` or `CANCELLED`. Order Service does not return `StockUnavailable` synchronously; clients read the result with `GET /orders/{orderId}`.
>
> **Payment (O4).** `paymentStatus` must be `PAID` or `FAILED`; anything else returns `ValidationFailed` (400). When the order becomes `PROCESSING`, `paymentDueAt` is set to 10 minutes later (configurable). A scheduled check in Order Service cancels `PROCESSING` orders whose `paymentDueAt` has passed and publishes `OrderCancelled`.
>
> **Order from a recommended build (R3).** `POST /orders/from-recommendation/{recommendationId}` adds every component of the build to the customer's cart with quantity 1, at the product's current Catalog price; the cart is created if the customer has none, otherwise the components are added to the existing cart. Recommendation Service's 401, 403 and 404 are passed on as `AuthenticationRequired`, `AccessDenied` and `ResourceNotFound`; a component that is no longer in the catalog returns `ResourceNotFound` (404).
>
> When Account, Catalog or Recommendation Service is unavailable, returns a 5xx or times out, the request fails with `ServiceUnavailable` (503). Account Service's 401, 403 and 404 on the address call are passed on as the same errors. `GET /users/{userId}/orders` returns the newest order first.

### Account Service (http://localhost:8084/api)

| Feature | Method | Endpoint Path | Request Body | Response (Success) | Description |
|---|---|---|---|---|---|
| A1 | POST | /users | UserRegistrationRequest | 201 Created (UserResponse) | Register a new customer account |
| A1 | GET | /users/{userId} | None | 200 OK (UserResponse) | Retrieve account and profile information |
| A1 | PUT | /users/{userId}/profile | ProfileUpdateRequest | 200 OK (UserResponse) | Update customer profile information |
| A1 | GET | /users/{userId}/addresses | None | 200 OK (Array\<AddressResponse\>) | Retrieve saved delivery addresses |
| A1 | POST | /users/{userId}/addresses | AddressCreateRequest | 201 Created (AddressResponse) | Add a saved delivery address |
| A1 | PUT | /users/{userId}/addresses/{addressId} | AddressUpdateRequest | 200 OK (AddressResponse) | Update a saved delivery address |
| A1 | DELETE | /users/{userId}/addresses/{addressId} | None | 204 No Content | Remove a saved delivery address |
| A2 | POST | /auth/login | LoginRequest | 200 OK (AuthenticationResponse) | Authenticate a registered customer |
| A3 | GET | /users/{userId}/preferences | None | 200 OK (PreferenceResponse) | Retrieve saved recommendation preferences |
| A3 | PUT | /users/{userId}/preferences | PreferenceUpdateRequest | 200 OK (PreferenceResponse) | Save or update recommendation preferences |

#### Account Service DTOs (A1)

Field names and types follow [User](domain-model.md#41-user), [UserProfile](domain-model.md#42-userprofile) and [Address](domain-model.md#44-address).

| DTO | Fields |
|---|---|
| UserRegistrationRequest | username, email, phone, password, firstName (optional), lastName (optional) |
| UserResponse | id, username, email, phone, firstName, lastName |
| ProfileUpdateRequest | firstName (optional), lastName (optional) |
| AddressCreateRequest | streetNo (Integer), streetName, suburb, city, state, postCode (Integer) |
| AddressUpdateRequest | streetNo (Integer), streetName, suburb, city, state, postCode (Integer) |
| AddressResponse | id, streetNo, streetName, suburb, city, state, postCode |

> `POST /users` returns `ValidationFailed` (400) when `username`, `email`, `phone` or `password` is missing or blank, or the username is already taken; it always creates a `CUSTOMER`. `UserResponse` never contains the password or its hash, and not the role or the preference. `PUT /users/{userId}/profile` replaces the whole profile: a missing `firstName` or `lastName` is cleared. All address fields are required; `PUT /users/{userId}/addresses/{addressId}` replaces the whole address. An address id that does not exist or belongs to another user returns `ResourceNotFound` (404). `AddressResponse.id` is the `deliveryAddressId` used by Order Service (O2).

#### Account Service DTOs (A2)

| DTO | Fields |
|---|---|
| LoginRequest | username, password |
| AuthenticationResponse | token, tokenType (`Bearer`), expiresAt (ISO-8601 UTC timestamp), userId, role (`CUSTOMER` / `ADMIN`) |

> `POST /auth/login` returns `ValidationFailed` (400) when `username` or `password` is missing or blank, and `AuthenticationRequired` (401) for an unknown username or a wrong password, with the same message for both. The administrator account logs in through the same endpoint. See [Authentication and Authorisation](#authentication-and-authorisation).

#### Account Service DTOs (A3)

Field names and types follow [Preference](domain-model.md#43-preference).

| DTO | Fields |
|---|---|
| PreferenceUpdateRequest | preference (free-text String, optional) |
| PreferenceResponse | userId, preference (`null` when no preference is saved) |

> `GET /users/{userId}/preferences` returns 200 with `preference` = `null` when the user has not saved a preference, and `ResourceNotFound` (404) for an unknown user. `PUT /users/{userId}/preferences` replaces the saved preference; a `preference` that is missing, `null` or blank clears it. The preference has no maximum length. `PUT` raises the domain event `PreferenceUpdated` only when the value changes; it is internal to Account Service and not published to Kafka.

### Recommendation Service (http://localhost:8085/api)

| Feature | Method | Endpoint Path | Request Body | Response (Success) | Description |
|---|---|---|---|---|---|
| R1 | POST | /recommendations | BuildRecommendationRequest | 201 Created (RecommendationResponse) | Generate a recommended PC build based on budget, intended use, and customer preferences |
| R2 | POST | /compatibility/validate | CompatibilityRequest | 200 OK (CompatibilityResponse) | Validate compatibility between selected PC components and report conflicts |
| R3 | GET | /recommendations/{recommendationId} | None | 200 OK (RecommendationResponse) | View a generated recommendation and its individual components |
| R4 | GET | /users/{userId}/recommendations | None | 200 OK (Array\<RecommendationResponse\>) | Retrieve a customer's previous PC build recommendations |

#### Recommendation Service DTOs

Field names and types follow the [Recommendation Service Domain Model](domain-model.md#5-recommendation-service-domain-model).

| DTO | Fields |
|---|---|
| BuildRecommendationRequest | userId, budget, purpose, preferences (optional) |
| RecommendationResponse | recommendationId, buildId, userId, budget, purpose, preferences, totalPrice, description, reasoning, components (Array of {productId, componentType, price}) |
| CompatibilityRequest | productIds (Array of String) |
| CompatibilityResponse | compatible (Boolean), conflicts (Array of {specName, productIds, message}) |

> `RecommendationResponse` combines the BuildRequest (`userId`, `budget`, `purpose`, `preferences`), the PCBuild (`buildId`, `totalPrice`, `description`, `components`) and the Recommendation (`recommendationId`, `reasoning`). R1, R3 and R4 return the same DTO; R4 returns one per previous build request.
>
> `POST /compatibility/validate` (R2) always returns 200 when the check can be made: `compatible` = `true` with an empty `conflicts` list, or `compatible` = `false` with the conflicts found by the rules in [Component Types and Compatibility Rules](domain-model.md#55-component-types-and-compatibility-rules). It does not return `CompatibilityConflict` (409). An empty or missing `productIds` list, two products with the same component type, or a product whose category is not a canonical component type returns `ValidationFailed` (400); a `productId` that is not in the catalog returns `ResourceNotFound` (404). R2 does not create a PCBuild and stores nothing.
>
> `POST /recommendations` (R1) returns `ValidationFailed` (400) when `budget` is missing or not greater than 0, or `purpose` is missing or blank; `preferences` is optional. The response's `preferences` is the preference from the request (the saved Account preference is only extra context for the LLM and is not stored). Recommendation Service validates the LLM's answer: every product ID exists in the catalog and is in stock, there is exactly one product of each of the seven component types, the build passes the same compatibility rules as R2, and the total price (from Catalog) is within the budget. If the answer is invalid, the LLM is asked once more with the validation errors; if the second answer is also invalid, R1 returns `CompatibilityConflict` (409). When an LLM call fails technically or times out, R1 returns `ServiceUnavailable` (503).
>
> `GET /users/{userId}/recommendations` (R4) does not guarantee any order.
>
> Recommendation Service reads products with Catalog `GET /products` and maps them locally (component type from the category name via `GET /categories`); it does not call `GET /products/{productId}`. It checks stock by calling Inventory `GET /inventory/{productId}` directly. Only in-stock products can be recommended (R1); a 404 from Inventory (no inventory record) means the product is not available. When Account Service (R1 preferences), Catalog Service (R1, R2) or Inventory Service (R1) is unavailable, returns a 5xx response or times out, the request fails with `ServiceUnavailable` (503). Errors from Account Service's `GET /users/{userId}/preferences` in R1 are passed on: 401 → `AuthenticationRequired` (401), 403 → `AccessDenied` (403), 404 → `ResourceNotFound` (404). See [Architecture Decision 12](architecture.md#architecture-decisions).

### Endpoint Naming and Contract Rules

- All REST endpoints use the `/api` base.
- GET endpoints are read-only and do not modify application state.
- POST is used when creating resources or performing explicit state-transition actions such as cancelling an order.
- PUT is used to update an existing resource or its state.
- DELETE is used to remove resources or release resources such as stock reservations.
- Resource names use plural nouns where applicable, such as `/products`, `/orders`, `/users`, and `/recommendations`.
- Resource identifiers are included as path parameters, for example `/products/{productId}`.
- Successful creation requests return 201 Created.
- Successful queries and updates return 200 OK.
- A request that is accepted but whose result arrives asynchronously returns 202 Accepted (order checkout).
- Successful deletions that do not require a response body return 204 No Content.
- Inter-service asynchronous communication uses Kafka events where specified by the system architecture.
- Account-specific and administrative endpoints require authentication and appropriate authorization — see [Authentication and Authorisation](#authentication-and-authorisation).

### Authentication and Authorisation

Agreed design for A2 / NFR-04 ([Architecture Decision 11](architecture.md#architecture-decisions)):

- `POST /auth/login` (Account Service) returns a JSON Web Token (JWT) in `AuthenticationResponse.token`.
- The token is signed with HS256 using a secret shared by all services (configuration property `security.jwt.secret`, same value in every service, at least 256 bits). Claims: `sub` (the user's `id`), `role` (`CUSTOMER` / `ADMIN`), `iat`, `exp`. Lifetime: 1 hour. There is no refresh token and no logout endpoint; a token is valid until it expires.
- Clients send the token on every non-public request in the header `Authorization: Bearer <token>`.
- Every service validates the token itself (signature and expiry) with Spring Security as a stateless OAuth2 resource server; there is no server-side session or token store and no call back to Account Service.

| Access | Rule | Endpoints |
|---|---|---|
| Public | No token required | Catalog: `GET /products`, `GET /categories`, `GET /products/{productId}`, `GET /products/trending`, `GET /products/{productId}/reviews`. Inventory: `GET /inventory/{productId}`. Account: `POST /users`, `POST /auth/login`. Recommendation: `POST /compatibility/validate` |
| Customer (owner) | Valid token, and the token's `sub` must be the user the request is for: the `{userId}` path parameter, the `userId` in the request body, or the `userId` stored on the order / recommendation | Account: `GET /users/{userId}`, `PUT /users/{userId}/profile`, all `/users/{userId}/addresses` endpoints, `GET`/`PUT /users/{userId}/preferences`. Catalog: `POST /products/{productId}/reviews`. Inventory: `POST /inventory/{productId}/notifications`, `GET /users/{userId}/stock-alerts`. Order: all Order Service endpoints. Recommendation: `POST /recommendations`, `GET /recommendations/{recommendationId}`, `GET /users/{userId}/recommendations` |
| Admin | Valid token with `role` = `ADMIN` | Catalog C5: `POST /products`, `PUT`/`DELETE /products/{productId}`, `POST /categories`, `PUT`/`DELETE /categories/{categoryId}`. Inventory I3: `PUT /inventory/{productId}`. Inventory I1: `POST /inventory/{productId}/reservations`, `DELETE /inventory/{productId}/reservations/{reservationId}` (direct use and testing only, Architecture Decision 5) |

- A missing, malformed or expired token, or one with an invalid signature, on a non-public endpoint returns `AuthenticationRequired` (401).
- A valid token for another user, or a `CUSTOMER` token on an admin endpoint, returns `AccessDenied` (403). The `ADMIN` role grants the admin endpoints only; it does not bypass the owner check.
- When the owner is given by `{userId}` or by the request body, the owner check happens before the resource is looked up (another user's id returns 403, not 404). When the owner is stored on the resource (order, recommendation), an unknown id returns 404 and another user's resource returns 403.
- Synchronous inter-service calls to protected endpoints forward the incoming `Authorization` header (Order → Account for saved addresses, Order → Recommendation for R3, Recommendation → Account for preferences). Calls to public endpoints (Catalog → Inventory, Order/Recommendation → Catalog, Recommendation → Inventory) need no token. Kafka events do not carry tokens.
- `GET /inventory/{productId}` stays public because Catalog Service calls it for public product details (C2).
- The H2 console is a development tool and is not protected.

### Error Response Contract

Every error response has the JSON body `{ "code": "<Error Code>", "message": "<human-readable explanation>" }`, where `code` is one of the error codes below.

| Error Code | HTTP Status | Applies To | Meaning |
|---|---|---|---|
| ValidationFailed | 400 Bad Request | All services | Request body, query parameter, or path parameter is invalid |
| AuthenticationRequired | 401 Unauthorized | Protected endpoints, `POST /auth/login` | Authentication is missing or invalid: no token, or a malformed, expired or wrongly signed token; or wrong login credentials |
| AccessDenied | 403 Forbidden | Protected/admin endpoints | Authenticated user does not have permission to perform the operation: not the owner of the resource, or not `ADMIN` for an admin endpoint |
| ResourceNotFound | 404 Not Found | All services | Requested resource does not exist |
| StockUnavailable | 409 Conflict | Inventory, Order | Requested quantity cannot be reserved because insufficient stock is available |
| ReservationConflict | 409 Conflict | Inventory | Stock reservation is invalid, expired, or cannot be modified |
| OrderStateViolation | 409 Conflict | Order | Requested operation is not valid for the current order state |
| CompatibilityConflict | 409 Conflict | Recommendation | No valid PC build (compatible, in stock, within the budget) can be generated for the request (R1); not used by `POST /compatibility/validate` |
| CategoryInUse | 409 Conflict | Catalog | Category still has products and cannot be removed |
| InternalServiceError | 500 Internal Server Error | All services | Unexpected service-side failure |
| ServiceUnavailable | 503 Service Unavailable | Inter-service operations, Recommendation LLM calls | Required downstream service or the LLM provider is temporarily unavailable or timed out |
