# PC Builder Microservices

CSCI318 Software Engineering Practices & Principles – group project.

## Microservices

| Service | Port | Status | Responsibilities |
|---|---|---|---|
| `account-service` | 8081 | placeholder | users, profiles, addresses, preferences |
| `catalog-service` | 8082 | skeleton | products, categories, specifications, prices, product reviews |
| `inventory-service` | 8083 | placeholder | stock, inventory items, stock reservations |
| `order-service` | 8084 | placeholder | cart, checkout, orders, order items, order/payment status |
| `recommendation-service` | 8085 | placeholder | AI-generated PC builds, compatibility checking, previous build requests |

Each service uses the base path `/api/v1`.

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

H2 console (catalog-service): http://localhost:8082/api/v1/h2-console (JDBC URL `jdbc:h2:mem:catalogdb`, user `sa`, no password).
