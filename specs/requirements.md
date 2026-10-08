# Requirements

> Source: `source-docs/Non_Functional Requirements-2.docx`

## Functional Requirements

| ID | Requirement | Service(s) | Related Objective |
|---|---|---|---|
| FR-01 | The system shall allow customers to browse and search the product catalog by category, brand and specification. | Catalog | Enables product discovery |
| FR-02 | The system shall display detailed product information, including specifications, price and current stock status. | Catalog, Inventory | Supports informed purchasing decisions |
| FR-03 | The system shall maintain and display a real-time list of trending products based on recent customer activity. | Catalog | Supports merchandising and conversion (real-time/streaming) |
| FR-04 | The system shall allow customers to submit and view product reviews and ratings. | Catalog | Provides social proof to support purchasing decisions |
| FR-05 | The system shall provide administrative functions to create, update and remove products and categories. | Catalog | Maintains catalog accuracy |
| FR-06 | The system shall reserve stock for items in a cart during checkout and release the reservation automatically if checkout is not completed within a defined time period. | Inventory, Order | Prevents overselling of limited stock |
| FR-07 | The system shall notify customers in real time when a product's stock falls below a defined threshold or is restocked. | Inventory | Reduces missed sales opportunities (real-time/streaming) |
| FR-08 | The system shall provide administrative functions to update stock levels. | Inventory | Maintains inventory accuracy |
| FR-09 | The system shall allow customers to add products to a cart (a DRAFT order) and complete checkout. | Order | Enables the core purchase transaction |
| FR-10 | The system shall allow customers to select a saved delivery address during checkout. | Order, Account | Connects account data to the order workflow |
| FR-11 | The system shall update order fulfilment status through asynchronous events exchanged between the Order and Inventory services. | Order, Inventory | Maintains service decoupling (event-driven architecture) |
| FR-12 | The system shall process and record payment status as part of the order lifecycle. | Order | Completes the transaction workflow (event-driven architecture) |
| FR-13 | The system shall allow customers to view their order history and track the status of current orders. | Order, Account | Builds customer trust and transparency |
| FR-14 | The system shall allow customers to cancel an order that has not yet been fulfilled. | Order | Supports the full order lifecycle |
| FR-15 | The system shall allow customers to register an account and manage their profile and address details. | Account | Enables repeat purchases and streamlined checkout |
| FR-16 | The system shall allow registered customers to authenticate (log in) before accessing account-specific features. | Account | Secures account-specific functionality |
| FR-17 | The system shall allow customers to save preferences to be used by the recommendation service. | Account, Recommendation | Personalises AI-generated recommendations |
| FR-18 | The system shall generate a recommended PC build based on a customer's budget, intended use and stated preferences, using an AI agent with access to live catalog and inventory data. | Recommendation | Removes the need for specialist technical knowledge (agentic AI) |
| FR-19 | The system shall validate the compatibility of a customer-assembled set of components and report any conflicts identified. | Recommendation | Reduces incompatible purchases (agentic AI) |
| FR-20 | The system shall allow customers to view the individual components of a recommended PC build and add the full build to their cart. | Recommendation, Order | Converts AI recommendations into actual sales |
| FR-21 | The system shall allow customers to view their previous build requests and the recommendations generated for them. | Recommendation | Supports revisiting earlier recommendations |

> Cart: there is no separate Cart entity or service. The cart referred to in FR-06, FR-09 and FR-20 is an Order in `DRAFT` status — see [Order Lifecycle](domain-model.md#33-order-lifecycle).
>
> FR-06: stock is reserved after checkout (order `PLACED`). The time limit applies to payment: if payment is not recorded within it, the order is cancelled and the reservation is released — see [Architecture Decisions](architecture.md#architecture-decisions).

## Non Functional Requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-01 | Performance | Real-time stock alerts and trending product updates shall reflect underlying events within a few seconds of occurrence. |
| NFR-02 | Scalability | Each microservice shall be independently deployable and horizontally scalable without requiring changes to other services. |
| NFR-03 | Reliability | The system shall continue to operate in a degraded state if an individual microservice becomes unavailable, consistent with the event-driven architecture. |
| NFR-04 | Security | Account data and administrative endpoints shall require authentication and authorisation before access is granted. |
| NFR-05 | Interoperability | All REST APIs shall follow a consistent, documented contract across services to support integration and testing. |
| NFR-06 | Maintainability | All services shall adhere to a consistent layered architecture and apply Domain-Driven Design patterns uniformly. |
| NFR-07 | Modifiability | Asynchronous inter-service communication shall use well-defined, versioned event schemas; synchronous reads may use documented REST APIs. |
| NFR-08 | Testability | Core business flows, including asynchronous event-driven flows, shall be covered by automated tests. |
| NFR-09 | Usability | The AI recommendation agent shall return a result within an acceptable response time and provide a human-readable explanation for its recommendation. |
