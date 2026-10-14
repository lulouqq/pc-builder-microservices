# PC Builder Microservices

CSCI318 Software Engineering Practices & Principles – group project.

## Microservices

| Service | Port | Status | Responsibilities |
|---|---|---|---|
| `catalog-service` | 8081 | C1 - C5 implemented | products, categories, specifications, prices, product reviews |
| `inventory-service` | 8082 | I1 - I3 implemented | stock, inventory items, stock reservations, stock alerts |
| `order-service` | 8083 | O1 - O6 and R3 implemented | cart, checkout, orders, order items, order/payment status |
| `account-service` | 8084 | A1 - A3 implemented | users, profiles, addresses, preferences |
| `recommendation-service` | 8085 | R1 - R4 implemented | AI-generated PC builds, compatibility checking, previous build requests |

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
- Spring Security with JWT (stateless OAuth2 resource server, HS256 tokens issued by account-service)
- H2 in-memory database for development
- Apache Kafka with Spring for Apache Kafka (order-service, inventory-service, catalog-service)
- LangChain4j for the AI recommendation agent (recommendation-service)

## Running

From the repository root:

```bash
./mvnw clean test
JWT_SECRET='<at least 32 bytes>' ./mvnw spring-boot:run -pl catalog-service
```

On Windows PowerShell:

```powershell
.\mvnw.cmd clean test
$env:JWT_SECRET='<at least 32 bytes>'; .\mvnw.cmd spring-boot:run -pl catalog-service
```

`JWT_SECRET` must be the same in every service (see the service READMEs for other required variables).

H2 console (catalog-service): http://localhost:8081/api/h2-console (JDBC URL `jdbc:h2:mem:catalogdb`, user `sa`, no password).

### Kafka

order-service and inventory-service exchange events through Apache Kafka, inventory-service uses it for stock alerts, and catalog-service uses it for the trending list (see [Kafka Event Contracts](specs/architecture.md#kafka-event-contracts)). Start a local broker on `localhost:9092` before running them:

```bash
docker compose up -d
```

Another broker can be used by setting `KAFKA_BOOTSTRAP_SERVERS`. Setting `KAFKA_ENABLED=false` runs a service without a broker. The tests do not need a running broker.

## Prompt History

- Project-wide AI prompts (setup, architecture, specifications): [`PROMPT_HISTORY.md`](PROMPT_HISTORY.md)
- Service-specific AI prompts: `<service>/PROMPT_HISTORY.md` in each microservice directory
