# Domain Model

> Source: `source-docs/Domain Model-3.docx`

![Domain model overview](images/domain-model-overview.png)

*Overview diagram: class diagram of all five service domains (Account, Order, Inventory, Catalog, Recommendation) with the fields listed below and the associations between them — e.g. User 1 — 0..1 UserProfile / 0..1 Preference / 0..\* Address, User 1 — 0..\* Order, Order 1 — 1..\* OrderItem, OrderItem 0..\* — 1 Product, Order 1 — 0..\* StockReservation, InventoryItem 1 — 0..\* StockReservation, InventoryItem 1 — 1 Product, Product 0..\* — 1 Category, Product 1 — 1..\* Specification, User 1 — 0..\* BuildRequest, BuildRequest 1 — 1 PCBuild, PCBuild 1 — 1..\* BuildComponent, BuildComponent 0..\* — 1 Product, PCBuild 1 — 0..1 Recommendation. The diagram's "User Service Domain" label refers to the Account Service.*

> The diagrams show User 1 — 1 Address; the relationship used in the specs is User 1 — 0..\* Address (a user can have several saved delivery addresses, A1). The diagrams also predate these additions, which are defined only in the tables below: `Review` (1.4), `StockNotificationSubscription` (2.3), `Order.deliveryAddressId` and `Order.paymentStatus` (3.1), `User.id`, `User.passwordHash` and `User.role` (4.1), `Address.id` (4.4), `BuildComponent.id` (5.3), and `Product.categoryId` and `Product.specifications` (1.1), which record the Product — Category and Product — Specification associations as fields.

## Domain Classes and Link to Requirements

| Service | Related Requirements | Domain Classes |
|---|---|---|
| Catalog Service | FR-01, FR-02, FR-03, FR-04, FR-05 | Product, Category, Specification, Review |
| Inventory Service | FR-02, FR-06, FR-07, FR-08, FR-11 | InventoryItem, StockReservation, StockNotificationSubscription |
| Order Service | FR-06, FR-09, FR-10, FR-11, FR-12, FR-13, FR-14, FR-20 | Order, OrderItem |
| Account Service | FR-10, FR-13, FR-15, FR-16, FR-17 | User, UserProfile, Preference, Address |
| Recommendation Service | FR-17, FR-18, FR-19, FR-20, FR-21 | BuildRequest, PCBuild, BuildComponent, Recommendation |

| Domain Class | Description | Type |
|---|---|---|
| Product | PC product or component available in the product catalog | Aggregate Root / Entity |
| Category | Product category used to organise related PC components | Aggregate Root / Entity |
| Specification | Technical attributes and specifications of a product | Value Object |
| Review | Customer review and rating of a product | Aggregate Root / Entity |
| InventoryItem | Stock record for a product; maintains quantity and availability | Aggregate Root / Entity |
| StockReservation | Temporary stock allocation created during checkout | Entity |
| StockNotificationSubscription | Customer registration to receive stock notifications for a product (CRUD, not part of the event-sourced InventoryItem) | Aggregate Root / Entity |
| Order | Customer purchase; manages the order lifecycle and fulfilment status. A DRAFT order acts as the customer's cart | Aggregate Root / Entity |
| OrderItem | Individual product and quantity included in an order | Entity |
| User | Registered customer account and authentication information | Aggregate Root / Entity |
| UserProfile | Personal profile details associated with a user | Value Object |
| Preference | Saved customer preferences used for PC build recommendations | Value Object |
| Address | Saved customer delivery address | Entity |
| BuildRequest | Customer request containing budget, intended use, and preferences for a PC build | Aggregate Root / Entity |
| PCBuild | Complete PC configuration consisting of selected components | Aggregate Root / Entity |
| BuildComponent | Individual PC component included in a build | Entity |
| Recommendation | Generated PC build recommendation associated with a customer request | Entity |

## 1. Catalog Service Domain Model

### 1.1 Product

| Field Name | Type | Description | Constraints |
|---|---|---|---|
| id | String | Unique Identifier of each product | Primary Identifier |
| name | String | Product Name | Required |
| price | Float | Price of the item | Required |
| brand | String | Brand the item belongs to | Required |
| categoryId | String | Identifier of the Category the product belongs to (Product 0..\* — 1 Category) | Required |
| specifications | List\<Specification\> | Technical specifications of the product (Product 1 — 1..\* Specification) | Required (at least one) |

### 1.2 Category

| Field Name | Type | Description | Constraints |
|---|---|---|---|
| id | String | Unique identifier of the category of item | Primary Identifier |
| name | String | Name of category item belongs to | Required |

### 1.3 Specification

| Field Name | Type | Description | Constraints |
|---|---|---|---|
| name | String | Name of the specification | Required |
| value | String | Specification Value | Required |

### 1.4 Review

| Field Name | Type | Description | Constraints |
|---|---|---|---|
| id | String | Unique identifier of each review | Primary Identifier |
| productId | String | Identifier of the reviewed product | Required |
| userId | String | Identifier of the user who submitted the review | Required |
| rating | Integer | Rating given to the product, in stars | Required, 1–5 |
| comment | String | Review text | Optional |
| createdAt | DateTime | Date and Time the review was submitted | Required |

### 1.5 Catalog Domain Events

| Event | Triggered By | Key Payload Fields | Published to Kafka |
|---|---|---|---|
| ProductCreated | `POST /products` (C5) | productId, name, price, brand, categoryId | No |
| ProductUpdated | `PUT /products/{productId}` (C5) | productId, name, price, brand, categoryId | No |
| ProductRemoved | `DELETE /products/{productId}` (C5) | productId | No |
| ProductReviewed | `POST /products/{productId}/reviews` (C4) | reviewId, productId, userId, rating | No |
| ProductViewed | `GET /products/{productId}` (C2) | productId | Yes |

Every event also carries `occurredAt` (DateTime). `ProductCreated`, `ProductUpdated`, `ProductRemoved` and `ProductReviewed` are domain events internal to Catalog Service. `ProductViewed` is the only Catalog event published to Kafka; Catalog Service consumes it to maintain the trending list (C3) — see [Architecture Decisions](architecture.md#architecture-decisions).

![Catalog service domain model](images/domain-model-catalog.png)

## 2. Inventory Service Domain Model

### 2.1 InventoryItem

| Field Name | Type | Description | Constraints |
|---|---|---|---|
| id | String | Unique identifier of each item | Primary Identifier |
| productId | String | Identifier of the product in the Catalog Service | Required |
| quantity | Integer | Total quantity of the product currently in stock | Required |
| reservedQuantity | Integer | Quantity currently reserved for orders | Required |

### 2.2 StockReservation

| Field Name | Type | Description | Constraints |
|---|---|---|---|
| id | String | Unique identifier of each reservation | Primary Identifier |
| productId | String | Identifier of the product being reserved | Required |
| orderId | String | Identifier of the order the stock is reserved for | Required |
| quantity | Integer | Quantity of the product reserved for the order | Required |
| status | String | Current status of the reservation (ACTIVE/RELEASED/CONFIRMED): ACTIVE after `StockReserved`, RELEASED after `StockReservationReleased`, CONFIRMED after `StockReservationConfirmed` | Required |

### 2.3 StockNotificationSubscription

| Field Name | Type | Description | Constraints |
|---|---|---|---|
| id | String | Unique identifier of each subscription | Primary Identifier |
| productId | String | Identifier of the product to receive notifications for | Required |
| userId | String | Identifier of the user to notify | Required |
| createdAt | DateTime | Date and Time of the subscription | Required |

![Inventory service domain model](images/domain-model-inventory.png)

## 3. Order Service Domain Model

### 3.1 Order

| Field Name | Type | Description | Constraints |
|---|---|---|---|
| id | String | Unique identifier of each order | Primary Identifier |
| userId | String | User ID of the account the order was placed by | Required |
| status | String | Status of the order (DRAFT/PLACED/PROCESSING/COMPLETED/CANCELLED) — see 3.3 | Required |
| totalPrice | Float | Total price of all order items | Required |
| createdAt | DateTime | Date and Time of the order | Required |
| deliveryAddressId | String | Identifier of the saved Address (Account Service) selected for delivery (O2) | Optional |
| paymentStatus | String | Status of the payment (PENDING/PAID/FAILED) (O4) | Required |
| paymentDueAt | DateTime | Time by which payment must be recorded; set when the order becomes PROCESSING (10 minutes later). An unpaid order is cancelled after it (FR-06) | Optional |

### 3.2 OrderItem

| Field Name | Type | Description | Constraints |
|---|---|---|---|
| id | String | Unique identifier of order item | Primary Identifier |
| productId | String | Unique identifier of product | Required |
| quantity | Integer | Number of order items | Required |
| unitPrice | Float | Price of each order item | Required |

### 3.3 Order Lifecycle

There is no separate Cart entity or service: the Order aggregate also represents the shopping cart before checkout.

```text
DRAFT -> PLACED -> PROCESSING -> COMPLETED
  |        |           |
  +--------+-----------+------> CANCELLED
```

| Status | Meaning |
|---|---|
| DRAFT | The customer's cart. OrderItems can be added and removed only in this status. |
| PLACED | Checkout completed (DRAFT → PLACED). The order / inventory / Kafka flow begins (`OrderPlaced` is published). |
| PROCESSING | Stock has been reserved for the order (`StockReserved` received); waiting for payment. |
| COMPLETED | Payment recorded successfully; the order has been fulfilled. |
| CANCELLED | The order was cancelled before it was fulfilled (FR-14). |

**Transitions**

| From | To | Trigger | Kafka event published |
|---|---|---|---|
| — | DRAFT | `POST /orders` or `POST /orders/from-recommendation/{recommendationId}` | — |
| DRAFT | PLACED | Checkout: `POST /orders/{orderId}/checkout` | `OrderPlaced` |
| PLACED | PROCESSING | `StockReserved` received from Inventory Service | — |
| PLACED | CANCELLED | `StockReservationFailed` received from Inventory Service | — |
| PROCESSING | COMPLETED | Successful payment recorded (`paymentStatus` = PAID): `PUT /orders/{orderId}/payment` | `OrderCompleted` |
| PROCESSING | CANCELLED | Failed payment recorded (`paymentStatus` = FAILED), or no payment recorded within the reservation time limit | `OrderCancelled` |
| DRAFT, PLACED, PROCESSING | CANCELLED | Customer cancels: `POST /orders/{orderId}/cancel` (FR-14) | `OrderCancelled` (from PLACED or PROCESSING only) |

- Stock is reserved after checkout, when Inventory Service consumes `OrderPlaced` (I1, O3). The reservation is temporary: if payment is not recorded within the time limit, Order Service cancels the order and publishes `OrderCancelled`, and Inventory Service releases the reservation (FR-06).
- When Inventory Service consumes `OrderCancelled`, it releases any stock reservation held for that order (`StockReservationReleased`).
- When Inventory Service consumes `OrderCompleted`, it confirms the stock reservation for that order (`StockReservationConfirmed`).
- A COMPLETED order cannot be cancelled.
- A customer can have only one DRAFT order at a time.
- Adding a product that is already in a DRAFT order increases the quantity of its OrderItem. `unitPrice` is the product's price in Catalog Service when the item was last added.
- Checkout requires at least one OrderItem and a `deliveryAddressId`.
- The payment time limit is 10 minutes from the moment the order becomes PROCESSING (`paymentDueAt`). A scheduled check in Order Service cancels overdue orders.

Operations that are not valid for the current status are rejected with `OrderStateViolation` (409 Conflict).

![Order service domain model](images/domain-model-order.png)

> Note: the diagrams label the OrderItem price field as `price`; the field name used across the specs is `unitPrice`, as in the table above. The diagrams do not show the order status values.

## 4. Account Service Domain Model

### 4.1 User

| Field Name | Type | Description | Constraints |
|---|---|---|---|
| id | String | Unique identifier of each user | Primary Identifier |
| username | String | Unique username of the user | Required |
| email | String | Email of user | Required |
| phone | String | Phone number of the user | Required |
| passwordHash | String | Hashed password used for authentication (A2); the plain password is never stored | Required |
| role | String | Role of the user (CUSTOMER/ADMIN), carried in the authentication token (A2, NFR-04): CUSTOMER for every account registered through `POST /users`; ADMIN only for the administrator account that Account Service creates at startup from configuration. It cannot be changed through the API | Required |

### 4.2 UserProfile

| Field Name | Type | Description | Constraints |
|---|---|---|---|
| firstName | String | First name of the user | Optional |
| lastName | String | Last name of the user | Optional |

### 4.3 Preference

| Field Name | Type | Description | Constraints |
|---|---|---|---|
| preference | String | User preferences for the recommendation service | Optional |

### 4.4 Address

| Field Name | Type | Description | Constraints |
|---|---|---|---|
| id | String | Unique identifier of each address | Primary Identifier |
| streetNo | Integer | Street number of the user’s address | Required |
| streetName | String | Street name of the user’s address | Required |
| suburb | String | Suburb of the user’s address | Required |
| city | String | City of the user’s address | Required |
| state | String | State of the user’s address | Required |
| postCode | Integer | Post code of the user’s address | Required |

![Account service domain model](images/domain-model-account.png)

> Note: the diagrams do not yet show `User.id`, `User.passwordHash`, `User.role` or `Address.id`; `User.id` is the primary identifier referenced as `userId` by Order, BuildRequest and the `/users/{userId}` endpoints.

## 5. Recommendation Service Domain Model

### 5.1 BuildRequest

| Field Name | Type | Description | Constraints |
|---|---|---|---|
| id | String | Unique identifier of each build request | Primary Identifier |
| userId | String | Identifier of the user requesting the PC build | Required |
| budget | Float | Maximum budget for the requested PC build | Required, greater than 0 |
| purpose | String | Intended use of the PC, such as gaming, work, or content creation | Required, not blank |
| preferences | String | Additional user preferences for the PC build, as given in the current request (R1). The preference saved in Account Service (A3) is not stored here; it is only extra context for the LLM | Optional |

### 5.2 PCBuild

| Field Name | Type | Description | Constraints |
|---|---|---|---|
| id | String | Unique identifier of each PC build | Primary Identifier |
| requestId | String | Identifier of the build request used to generate this build | Required |
| totalPrice | Float | Total price of all components in the PC build | Required |
| description | String | AI-generated summary of the PC build | Required |

### 5.3 BuildComponent

| Field Name | Type | Description | Constraints |
|---|---|---|---|
| id | String | Unique identifier of each build component | Primary Identifier |
| productId | String | Identifier of the product selected for the PC build | Required |
| componentType | String | Type of PC component: one of the canonical component types in 5.5 (CPU, Motherboard, RAM, GPU, Storage, PSU, Case) | Required |
| price | Float | Price of the component when the build was generated | Required |

### 5.4 Recommendation

| Field Name | Type | Description | Constraints |
|---|---|---|---|
| id | String | Unique identifier of each recommendation | Primary Identifier |
| buildId | String | Identifier of the PC build associated with the recommendation | Required |
| reasoning | String | AI-generated explanation of why the selected PC build is suitable for the user's requirements | Required |

### 5.5 Component Types and Compatibility Rules

**Canonical component types.** A product's component type is the name of its Catalog category. The canonical category names are:

`CPU`, `Motherboard`, `RAM`, `GPU`, `Storage`, `PSU`, `Case`

Catalog products used for recommendations must be in a category with one of these names. A generated build (R1) contains exactly one product of each of the seven types.

**Compatibility specification names.** Catalog products use these specification names (Catalog `Specification.name`) for compatibility checks:

| Specification name | Used on |
|---|---|
| Socket | CPU, Motherboard |
| Memory Type | RAM, Motherboard |
| Form Factor | Motherboard, Case |

**Compatibility rules (R1, R2).** Compatibility is checked with deterministic rules; a conflict is reported when the two products' values for the specification differ. R1 checks the generated build with the same rules as R2.

| Rule | Components | Specification |
|---|---|---|
| 1 | CPU ↔ Motherboard | Socket |
| 2 | RAM ↔ Motherboard | Memory Type |
| 3 | Motherboard ↔ Case | Form Factor |

There is no power (wattage) rule.

- A rule is checked only when both of its component types are among the selected products; otherwise it is skipped and is not a conflict.
- When both component types are present but one of the products has no value for the rule's specification, the components are not compatible (`compatible` = `false`) and the conflict's message says which specification is missing.
- Values are compared after trimming surrounding whitespace, ignoring case. Values are not interpreted (e.g. `ATX` and `Micro-ATX` are just different values); Catalog demo data must use one canonical value per specification for compatible parts.
- In R2, at most one product per component type may be selected, and every product must be in a canonical category; otherwise the request is rejected with `ValidationFailed` (400). R2 does not create a PCBuild or BuildComponents and stores nothing.

![Recommendation service domain model](images/domain-model-recommendation.png)

> Note: BuildRequest and PCBuild are separate aggregate roots, as in the table at the top of this document: BuildRequest is what the customer asked for, PCBuild is the build the AI generated. The diagram's composition between BuildRequest and PCBuild is out of date.
