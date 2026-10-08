# User Stories

> Source: `source-docs/User Stories-2.docx`
>
> Event names in this file follow the [Architecture Decisions](architecture.md#architecture-decisions): the checkout event is `OrderPlaced` (replaces `OrderCreated`), `CheckoutStarted` is not used, a failed reservation is reported with `StockReservationFailed`, and a paid order publishes `OrderCompleted`, which confirms the reservation (`StockReservationConfirmed`). Where the agreed order flow differs from the wording of a story, a note is added under that story; the story text itself is unchanged. The domain classes `Review` and `StockNotificationSubscription`, added to the [Domain Model](domain-model.md) after the source document, are included in the mapping for C4 and I2.

## User Story Mapping

| User Story | Primary Relevant Domain Classes | Triggering Domain Events |
|---|---|---|
| C1: Browse and search products | Product, Category, Specification | None |
| C2: View product details | Product, Specification, InventoryItem | None |
| C3: View trending products | Product | ProductViewed |
| C4: Submit and view product reviews | Product, Review | ProductReviewed (when rating submitted) |
| C5: Manage products and categories | Product, Category, Specification | ProductCreated, ProductUpdated, ProductRemoved |
| I1: Reserve Stock During Checkout | InventoryItem, StockReservation | StockReserved, StockReservationReleased, StockReservationConfirmed |
| I2: Receive Stock Notifications | InventoryItem, StockNotificationSubscription | StockLow, ProductRestocked |
| I3: Update Stock Levels | InventoryItem | StockLevelUpdated |
| O1: Add Products to Cart and Checkout | Order, OrderItem | OrderPlaced |
| O2: Select Delivery Address | Order, Address | None |
| O3: Update Order Fulfilment Status | Order, InventoryItem, StockReservation | OrderPlaced, StockReserved, StockReservationFailed |
| O4: Process Payment Status | Order | PaymentStatusUpdated, OrderCompleted |
| O5: View Order History and Track Orders | Order, User | None |
| O6: Cancel Order | Order, StockReservation | OrderCancelled, StockReservationReleased |
| A1: Register and Manage Account | User, UserProfile, Address | UserRegistered, UserProfileUpdated |
| A2: Authenticate Customer | User | None |
| A3: Save Recommendation Preferences | User, Preference | PreferenceUpdated |
| R1: Generate Recommended PC Build | BuildRequest, PCBuild, BuildComponent, Recommendation | BuildRequested, RecommendationGenerated |
| R2: Validate Component Compatibility | PCBuild, BuildComponent, Product, Specification | CompatibilityChecked |
| R3: Add Recommended Build to Cart | PCBuild, BuildComponent, Order, OrderItem | None |
| R4: View Previous Recommendations | BuildRequest, PCBuild, Recommendation | None |

## Catalog Service

### C1: Browse and Search Products

As a Customer, I want to browse and search products by category, brand, and specification, so that I can find PC components that meet my requirements.

**Acceptance rules:**

1. Customers can browse products by category.
2. Customers can search or filter products by brand.
3. Customers can search or filter products using relevant specifications.

### C2: View Product Details

As a Customer, I want to view information about a product, so that I can make an informed purchase.

**Scenario:**

1. The customer selects a product from the catalog.
2. Catalog Service retrieves the product's information and specifications.
3. Inter-Service Query: Current stock status is obtained from Inventory Service.
4. The system displays the product specifications, price, and current stock status.

### C3: View Trending Products

As a customer I want to view trending products, so that I can discover up to date products receiving customer interest.

**Acceptance rules:**

1. Trending product determined by customer activities
2. Trending list is updated in real time

### C4: Submit and View Product Reviews

As a Customer, I want to submit and view product reviews and ratings, so that I can share my experience and use other customers' feedback when making purchasing decisions.

**Acceptance rules:**

1. Customers can submit a rating and review for a product.
2. Customers can view ratings and reviews associated with a product.

### C5: Manage Products and Categories

As an Administrator, I want to create, update, and remove products and categories, so that the product catalog remains accurate.

**Acceptance rules:**

1. Only authorised administrators can access catalog management functions.
2. Administrators can create and update products and their specifications.
3. Administrators can remove products.
4. Administrators can create, update, and remove categories.

## Inventory Service

### I1: Reserve Stock During Checkout

As a Customer, I want stock to be temporarily reserved while I complete a checkout, so that the product is not sold to another customer during checkout.

**Scenario:**

1. Customer begins checkout
2. Inventory service verifies stock
3. StockReservation is created for required quantities
4. Reserved quantities prevented from showing up in search
5. If checkout is not complete within time limit, the stock reservation is released

> Agreed flow ([Architecture Decision 2](architecture.md#architecture-decisions)): stock is reserved after checkout (order `PLACED`), when Inventory Service consumes `OrderPlaced`. The time limit applies to payment: if payment is not recorded in time, the order is cancelled and the reservation is released via `OrderCancelled`.

### I2: Receive Stock Notifications

As a Customer, I want to receive real time notifications when a product has restocked or has low stock, so that I can purchase it in time.

**Acceptance rules:**

1. Relevant stock changes and generate inventory events
2. Customer notified when stock threshold reached or restocked
3. Notifications reflect stock changes in real time.

### I3: Update Stock Levels

As an Administrator, I want to update product stock levels, so that inventory remains accurate.

**Acceptance rules:**

1. Authorized personnels allowed to update stock
2. Update quantities reflected in inventory item

## Order Service

### O1: Add Products to Cart and Checkout

As a Customer, I want to add products to my cart and complete checkout, so that I can purchase PC components.

**Acceptance rules:**

1. Customers can add products to cart
2. Cart maintains selected products and quantities
3. Customer can initiate checkout for selected items

> Agreed flow ([Architecture Decisions 1 and 3](architecture.md#architecture-decisions)): the cart is an Order in `DRAFT` status. Checkout is a single `DRAFT → PLACED` transition that publishes `OrderPlaced`; `CheckoutStarted` is not used.

### O2: Select Delivery Address

As a Customer, I want to select my saved address during checkout, so that my order is delivered to the correct location.

**Scenario:**

1. Customer begins checkout
2. Order service obtains customer’s saved address from account service
3. Customer selects delivery address.
4. Selected address associated with the order

### O3: Update Order Fulfilment Status

As a Customer, I want my order fulfilment status to be updated automatically, so that I can track the progress of my order.

**Scenario:**

1. Customer completes an order.
2. Order Service publishes an order event.
3. Inventory Service processes the order and reserves the required stock.
4. Inventory Service returns the stock reservation result asynchronously.
5. Order Service updates the fulfilment status of the order.

> Agreed flow ([Architecture Decision 4](architecture.md#architecture-decisions)): the order event is `OrderPlaced`; the reservation result is `StockReserved` (order → `PROCESSING`) or `StockReservationFailed` (order → `CANCELLED`). See [Order Lifecycle](domain-model.md#33-order-lifecycle).

### O4: Process Payment Status

As a Customer, I want the payment status of my order to be recorded, so that I know whether my payment was successfully processed.

**Scenario:**

1. Customer proceeds with checkout.
2. Payment is processed as part of the order lifecycle.
3. Order Service records the payment status.
4. The order status reflects the result of the payment.

> Agreed flow ([Order Lifecycle](domain-model.md#33-order-lifecycle)): payment is recorded while the order is `PROCESSING`; a successful payment moves it to `COMPLETED` (publishes `OrderCompleted`, which confirms the stock reservation), a failed payment moves it to `CANCELLED` (publishes `OrderCancelled`).

### O5: View Order History and Track Orders

As a Customer, I want to view my previous orders and track current order statuses, so that I can monitor my purchases.

**Acceptance rules:**

1. Registered customers can view their previous orders.
2. Customers can view the current status of active orders.
3. Orders are associated with the correct customer account.

### O6: Cancel Order

As a Customer, I want to cancel an order that has not yet been fulfilled, so that I can stop an unwanted purchase.

**Acceptance rules:**

1. Customers can request cancellation of an unfulfilled order.
2. Fulfilled orders cannot be cancelled.
3. The order status is updated after successful cancellation.

## Account Service

### A1: Register and Manage Account

As a Customer, I want to register an account and manage my profile and address details, so that I can reuse my information for future purchases.

**Acceptance rules:**

1. Customers can create an account.
2. Customers can view and update their profile information.
3. Customers can add and update saved delivery addresses.

### A2: Authenticate Customer

As a Registered Customer, I want to log in to my account, so that I can securely access account-specific features.

**Acceptance rules:**

1. Registered customers can authenticate using their account credentials.
2. Account-specific functionality requires authentication.
3. Invalid authentication attempts do not grant access.

### A3: Save Recommendation Preferences

As a Customer, I want to save my PC preferences, so that they can be used when generating personalised PC build recommendations.

**Scenario:**

1. Customer enters or updates their preferences.
2. Account Service stores the preferences.
3. Recommendation Service can retrieve the saved preferences when generating a build.

## Recommendation Service

### R1: Generate Recommended PC Build

As a Customer, I want to receive a recommended PC build based on my budget, intended use and preferences, so that I can choose suitable components without specialist technical knowledge.

**Scenario:**

1. Customer submits a build request containing budget, intended use and preferences.
2. Recommendation Service retrieves relevant products from Catalog Service.
3. Recommendation Service checks live stock information from Inventory Service.
4. AI agent generates a PC build using the available information.
5. The recommendation and its explanation are returned to the customer.

### R2: Validate Component Compatibility

As a Customer, I want my selected PC components to be checked for compatibility, so that I can avoid purchasing incompatible parts.

**Scenario:**

1. Customer submits a set of selected components.
2. Recommendation Service retrieves relevant component specifications.
3. The AI agent evaluates compatibility between the components.
4. Any identified compatibility conflicts are reported to the customer.

### R3: Add Recommended Build to Cart

As a Customer, I want to view the individual components of a recommended PC build and add the complete build to my cart, so that I can easily purchase the recommendation.

**Acceptance rules:**

1. Customers can view all components included in the recommended build.
2. Customers can view the price of each component.
3. Customers can add the complete recommended build to their cart.
4. The selected components are passed to the Order workflow.

### R4: View Previous Recommendations

As a Customer, I want to view my previous build requests and generated recommendations, so that I can revisit earlier PC build suggestions.

**Acceptance rules:**

1. Customers can view their previous build requests.
2. Customers can view the recommendation generated for each request.
3. Previous recommendations remain associated with the correct customer.

## User Story to API Endpoint Mapping

| Story | Endpoints / Commands |
|---|---|
| C1 | `GET /products` |
| C2 | `GET /products/{productId}` (stock status via Inventory Service `GET /inventory/{productId}`) |
| C3 | `GET /products/trending` |
| C4 | `GET`, `POST /products/{productId}/reviews` |
| C5 | `POST /products`, `PUT`/`DELETE /products/{productId}`, `POST /categories`, `PUT`/`DELETE /categories/{categoryId}` |
| I1 | `POST /inventory/{productId}/reservations`, `DELETE /inventory/{productId}/reservations/{reservationId}`; ReserveStockCommand, ReleaseStockReservationCommand, ConfirmStockReservationCommand |
| I2 | `POST /inventory/{productId}/notifications`; SubscribeStockNotificationCommand |
| I3 | `PUT`, `GET /inventory/{productId}`; UpdateStockLevelCommand |
| O1 | `POST /orders`, `POST /orders/{orderId}/items`, `DELETE /orders/{orderId}/items/{orderItemId}`, `POST /orders/{orderId}/checkout` |
| O2 | `PUT /orders/{orderId}/delivery-address` |
| O3 | No REST endpoint — Kafka events `OrderPlaced`, `StockReserved`, `StockReservationFailed` (FR-11) |
| O4 | `PUT /orders/{orderId}/payment` |
| O5 | `GET /orders/{orderId}`, `GET /users/{userId}/orders` |
| O6 | `POST /orders/{orderId}/cancel` |
| A1 | `POST /users`, `GET /users/{userId}`, `PUT /users/{userId}/profile`, `/users/{userId}/addresses` endpoints |
| A2 | `POST /auth/login` |
| A3 | `GET`, `PUT /users/{userId}/preferences` |
| R1 | `POST /recommendations` |
| R2 | `POST /compatibility/validate` |
| R3 | `GET /recommendations/{recommendationId}`; `POST /orders/from-recommendation/{recommendationId}` (Order Service; listed under Feature ID `R3` in the Order Service section of [api-endpoints.md](api-endpoints.md)) |
| R4 | `GET /users/{userId}/recommendations` |
