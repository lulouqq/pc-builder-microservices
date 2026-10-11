# PC Builder Microservices

CSCI318 Software Engineering Practices & Principles – group project.

## Microservices

| Service | Port | Status | Responsibilities |
|---|---|---|---|
| `catalog-service` | 8081 | C1, C4, C5 implemented | products, categories, specifications, prices, product reviews |
| `inventory-service` | 8082 | placeholder | stock, inventory items, stock reservations |
| `order-service` | 8083 | placeholder | cart, checkout, orders, order items, order/payment status |
| `account-service` | 8084 | placeholder | users, profiles, addresses, preferences |
| `recommendation-service` | 8085 | placeholder | AI-generated PC builds, compatibility checking, previous build requests |

Each service uses the base path `/api` (see [specs/api-endpoints.md](specs/api-endpoints.md)).

## Specifications

The project specifications live in [`specs/`](specs/) (Markdown versions of the documents in `source-docs/`):

- [Requirements](specs/requirements.md) – functional (FR-01 … FR-21) and non-functional (NFR-01 … NFR-09) requirements
- [Domain Model](specs/domain-model.md) – domain classes, fields and diagrams per service
- [User Stories](specs/user-stories.md) – user stories (C1–C5, I1–I3, O1–O6, A1–A3, R1–R4), domain class/event mapping and endpoint mapping
- [API Endpoints](specs/api-endpoints.md) – REST endpoints, port mapping, contract rules and error contract
- [Event Sourcing](specs/event-sourcing.md) – event-sourced `InventoryItem` in inventory-service
- [CQRS](specs/cqrs.md) – command/query split in inventory-service
- [Architecture](specs/architecture.md) – service, REST and Kafka architecture diagram

## Layered Architecture

Each microservice is structured into four layers (package `com.csci318.<service>`):

1. `presentation` – REST controllers and DTOs
2. `service` – application services (use cases)
3. `domain` – entities, value objects, aggregate roots, domain events, domain services
4. `infrastructure` – repositories and technical adapters

## Technology Stack

- Java 21, Apache Maven (multi-module POM)
- Spring Boot 3.3 (Spring Web, Spring Data JPA)
- H2 in-memory database for development

## Running

From the repository root:

```bash
./mvnw clean test
./mvnw spring-boot:run -pl catalog-service
```

On Windows PowerShell:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run -pl catalog-service
```

H2 console (catalog-service): http://localhost:8081/api/h2-console (JDBC URL `jdbc:h2:mem:catalogdb`, user `sa`, no password).

## Prompt History

- Project-wide AI prompts (setup, architecture, specifications): [`PROMPT_HISTORY.md`](PROMPT_HISTORY.md)
- Service-specific AI prompts: `<service>/PROMPT_HISTORY.md` in each microservice directory
