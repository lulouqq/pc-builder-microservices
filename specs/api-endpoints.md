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
| ReviewCreateRequest | userId, rating, comment (optional) |
| ReviewResponse | id, productId, userId, rating, comment, createdAt |

> `ProductResponse.stockStatus` (`IN_STOCK` / `OUT_OF_STOCK` / `UNKNOWN`) is set only by `GET /products/{productId}` (C2), from Inventory Service `GET /inventory/{productId}`: `IN_STOCK` when `quantity − reservedQuantity` is greater than 0, otherwise `OUT_OF_STOCK`, and `UNKNOWN` when Inventory Service is unavailable or has no record for the product (NFR-03). It is `null` in list responses (C1, C3) and in C5 responses.
>
> `GET /products/{productId}` also publishes `ProductViewed` to Kafka for the trending list (C3); it does not change the product itself.

### Inventory Service (http://localhost:8082/api)

| Feature | Method | Endpoint Path | Request Body | Response (Success) | Description |
|---|---|---|---|---|---|
| I1 | POST | /inventory/{productId}/reservations | StockReservationRequest | 201 Created (StockReservationResponse) | Reserve available stock during checkout |
| I1 | DELETE | /inventory/{productId}/reservations/{reservationId} | None | 204 No Content | Release an existing stock reservation |
| I2 | POST | /inventory/{productId}/notifications | StockNotificationRequest | 201 Created (StockNotificationResponse) | Register a customer to receive stock notifications for a product |
| I3 | PUT | /inventory/{productId} | StockUpdateRequest | 200 OK (InventoryResponse) | Update the stock level of a product |
| I3 | GET | /inventory/{productId} | None | 200 OK (InventoryResponse) | Retrieve current stock information for a product |

> In the normal order flow, stock is reserved and released through Kafka events (`OrderPlaced` / `OrderCancelled`), not through these REST endpoints. The I1 REST endpoints are kept for direct use and testing — see [Architecture Decisions](architecture.md#architecture-decisions).

### Order Service (http://localhost:8083/api)

| Feature | Method | Endpoint Path | Request Body | Response (Success) | Description |
|---|---|---|---|---|---|
| O1 | POST | /orders | OrderCreateRequest | 201 Created (OrderResponse) | Create a DRAFT order (the customer's cart) |
| O1 | POST | /orders/{orderId}/items | OrderItemCreateRequest | 201 Created (OrderResponse) | Add a product to a DRAFT order (cart) |
| O1 | DELETE | /orders/{orderId}/items/{orderItemId} | None | 204 No Content | Remove a product from a DRAFT order (cart) |
| O1 | POST | /orders/{orderId}/checkout | None | 200 OK (OrderResponse) | Check out the order (DRAFT → PLACED) and begin the order / inventory flow |
| O2 | PUT | /orders/{orderId}/delivery-address | DeliveryAddressRequest | 200 OK (OrderResponse) | Select a saved delivery address for an order |
| O4 | PUT | /orders/{orderId}/payment | PaymentStatusRequest | 200 OK (OrderResponse) | Process and record the payment status of an order |
| O5 | GET | /orders/{orderId} | None | 200 OK (OrderResponse) | Retrieve and track a specific order |
| O5 | GET | /users/{userId}/orders | None | 200 OK (Array\<OrderResponse\>) | Retrieve a customer's order history |
| O6 | POST | /orders/{orderId}/cancel | None | 200 OK (OrderResponse) | Cancel an order that has not yet been fulfilled |
| R3 | POST | /orders/from-recommendation/{recommendationId} | None | 201 Created (OrderResponse) | Create a DRAFT order (cart) containing the components of a recommended PC build |

> The cart is an Order in `DRAFT` status; there are no separate cart endpoints. See [Order Lifecycle](domain-model.md#33-order-lifecycle). Item and checkout operations on an order that is not `DRAFT`, payment on an order that is not `PROCESSING`, and cancelling a `COMPLETED` order return `OrderStateViolation` (409).

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

### Recommendation Service (http://localhost:8085/api)

| Feature | Method | Endpoint Path | Request Body | Response (Success) | Description |
|---|---|---|---|---|---|
| R1 | POST | /recommendations | BuildRecommendationRequest | 201 Created (RecommendationResponse) | Generate a recommended PC build based on budget, intended use, and customer preferences |
| R2 | POST | /compatibility/validate | CompatibilityRequest | 200 OK (CompatibilityResponse) | Validate compatibility between selected PC components and report conflicts |
| R3 | GET | /recommendations/{recommendationId} | None | 200 OK (RecommendationResponse) | View a generated recommendation and its individual components |
| R4 | GET | /users/{userId}/recommendations | None | 200 OK (Array\<RecommendationResponse\>) | Retrieve a customer's previous PC build recommendations |

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
- Successful deletions that do not require a response body return 204 No Content.
- Inter-service asynchronous communication uses Kafka events where specified by the system architecture.
- Account-specific and administrative endpoints require authentication and appropriate authorization.

### Error Response Contract

| Error Code | HTTP Status | Applies To | Meaning |
|---|---|---|---|
| ValidationFailed | 400 Bad Request | All services | Request body, query parameter, or path parameter is invalid |
| AuthenticationRequired | 401 Unauthorized | Protected endpoints | Authentication is missing or invalid |
| AccessDenied | 403 Forbidden | Protected/admin endpoints | Authenticated user does not have permission to perform the operation |
| ResourceNotFound | 404 Not Found | All services | Requested resource does not exist |
| StockUnavailable | 409 Conflict | Inventory, Order | Requested quantity cannot be reserved because insufficient stock is available |
| ReservationConflict | 409 Conflict | Inventory | Stock reservation is invalid, expired, or cannot be modified |
| OrderStateViolation | 409 Conflict | Order | Requested operation is not valid for the current order state |
| CompatibilityConflict | 409 Conflict | Recommendation | Selected components contain compatibility conflicts |
| InternalServiceError | 500 Internal Server Error | All services | Unexpected service-side failure |
| ServiceUnavailable | 503 Service Unavailable | Inter-service operations | Required downstream service is temporarily unavailable |
