# Prompt History

This file records the history of AI-assisted development on this project.
For every substantial AI-assisted change, append an entry with: date, goal, exact user prompt, short summary of changes, and files created/modified.

---

## Prompt 1

**Date:** 2026-09-19

**Goal:** Create the initial repository skeleton (based on the reference repository structure) and set up `catalog-service` as a minimal Spring Boot project that builds and starts.

**Exact user prompt:**

````text
We are starting implementation of a university group project for CSCI318 Software Engineering Practices & Principles.

Use this repository/branch as the primary implementation reference:
https://github.com/gxshub/book-and-library-management-system-v1/tree/iter2-impl

Important:
- Use it as a structural and implementation reference only.
- Follow similar repository layout, Spring Boot conventions, package/layer organization, naming style, and implementation patterns where applicable.
- Do NOT blindly copy business logic from the library/book domain.
- Adapt the structure to our own PC-builder domain.

Our project consists of 5 microservices:

1. account-service
   - users
   - profiles
   - addresses
   - preferences

2. catalog-service
   - products
   - categories
   - specifications
   - prices
   - product reviews

3. inventory-service
   - stock
   - inventory items
   - stock reservations

4. order-service
   - cart
   - checkout
   - orders
   - order items
   - order/payment status

5. recommendation-service
   - AI-generated PC builds
   - compatibility checking
   - previous build requests/recommendations

Technology stack:
- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- H2 for local development/testing
- Kafka / Spring Cloud Stream later
- LangChain4j later for the recommendation service

Architecture:
Each microservice should follow the same layered structure used in our design:

- presentation
- service
- domain
- infrastructure

DDD concepts will include where appropriate:
- Entities
- Value Objects
- Aggregate Roots
- Domain Events
- Domain Services

For now, DO NOT:
- implement Kafka
- implement LangChain4j
- implement external LLM integration
- implement the whole project
- add unnecessary complexity
- introduce infrastructure or libraries that are not currently needed

FIRST TASK ONLY:

1. Inspect the reference repository and understand its implementation structure.
2. Create the initial skeleton for our repository based on the same general approach.
3. Create the 5 service directories:
   - account-service
   - catalog-service
   - inventory-service
   - order-service
   - recommendation-service
4. Start implementation setup with catalog-service only.
5. Set up catalog-service as a minimal Spring Boot project using:
   - Java 21
   - Spring Web
   - Spring Data JPA
   - H2
6. Create only the base package/layer structure for catalog-service:
   - presentation
   - service
   - domain
   - infrastructure
7. Do not implement business features yet unless something minimal is required for the project to compile/start.
8. Make sure catalog-service can build and start successfully.

Prompt history requirement:

Create a file in the repository root called:

PROMPT_HISTORY.md

Use it to preserve the history of AI-assisted development.

For every substantial AI-assisted change, append:
- Date
- Goal
- Exact user prompt
- Short summary of changes made
- Files created/modified

Add this current prompt as Prompt 1.

Very important workflow rules:

- Do NOT commit or push anything.
- Leave all changes uncommitted.
- Do NOT add Co-Authored-By or any AI-related Git trailers.
- Stop after completing this first setup task.
- Before making large assumptions, prefer following the reference repository.
- If the reference implementation does something differently from your usual preferred approach, explain the difference before deviating from it.
- Keep the implementation simple and aligned with what would be expected in a university lab/project.

At the end, provide a short report with:
1. what you inspected in the reference repository
2. what you created
3. repository structure
4. whether catalog-service builds/runs successfully
5. any decisions or assumptions made
6. what the logical next step should be

Do not continue into the next development task until I explicitly ask.
````

**Summary of changes:**

- Inspected the reference repository (`iter2-impl` branch): Maven multi-module root POM with `spring-boot-starter-parent` 3.3.2, one module per microservice, package `com.csci318.<service>`, `application.yml` with H2 in-memory DB and `/api/v1` context path, Maven wrapper at the root.
- Created a Maven multi-module root POM (`pc-builder-microservices`) following the same approach; only `catalog-service` is registered as a module for now.
- Copied the Maven wrapper (`mvnw`, `mvnw.cmd`, `.mvn/`) and `.gitignore` from the reference repository.
- Created the five service directories; the four not-yet-implemented services contain a placeholder `README.md` only.
- Set up `catalog-service` as a minimal Spring Boot app (Spring Web, Spring Data JPA, H2) with the four layer packages `presentation`, `service`, `domain`, `infrastructure` (each with a `package-info.java` so they are kept in Git), `application.yml` (port 8082, context path `/api/v1`, H2 `catalogdb`), and a context-load test.
- Verified: `./mvnw clean test` passes and `./mvnw spring-boot:run -pl catalog-service` starts on port 8082.

**Files created/modified:**

- `pom.xml`
- `README.md`
- `PROMPT_HISTORY.md`
- `.gitignore`
- `mvnw`, `mvnw.cmd`, `.mvn/wrapper/*`
- `catalog-service/pom.xml`
- `catalog-service/src/main/java/com/csci318/catalogservice/CatalogServiceApplication.java`
- `catalog-service/src/main/java/com/csci318/catalogservice/{presentation,service,domain,infrastructure}/package-info.java`
- `catalog-service/src/main/resources/application.yml`
- `catalog-service/src/test/java/com/csci318/catalogservice/CatalogServiceApplicationTests.java`
- `account-service/README.md`, `inventory-service/README.md`, `order-service/README.md`, `recommendation-service/README.md`
