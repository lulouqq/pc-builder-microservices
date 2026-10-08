# CLAUDE.md

Project rules for AI-assisted work on the PC Builder Microservices project (CSCI318 group project).

## Source of truth

- `specs/` is the source of truth for requirements, domain model, user stories, API endpoints, event sourcing, CQRS and architecture.
- `source-docs/` holds the original DOCX/PNG files the specs were converted from; do not use it instead of `specs/`.
- Do not invent requirements, endpoints, events, entities or architecture that are not in `specs/`.
- If code, specs or a request conflict, report the conflict and suggest the smallest fix instead of silently resolving it.

## Services

There are exactly 5 microservices — do not add or merge services:

| Service | Port | Base path |
|---|---|---|
| `catalog-service` | 8081 | `/api` |
| `inventory-service` | 8082 | `/api` |
| `order-service` | 8083 | `/api` |
| `account-service` | 8084 | `/api` |
| `recommendation-service` | 8085 | `/api` |

## Technology

- Java 21, Spring Boot, Spring Data JPA, H2 (Maven multi-module build).
- Kafka for asynchronous inter-service events.
- LangChain4j for the recommendation-service AI agent.
- Do not add other libraries unless the task requires them.

## Architecture and design

- Every service follows the same layered structure (package `com.csci318.<service>`): `presentation`, `service`, `domain`, `infrastructure`.
- Apply DDD where the domain model specifies it: entities, value objects, aggregate roots, domain events, domain services.
- Keep it simple and appropriate for a university project — do not over-engineer.

## Workflow

- Do not commit or push unless explicitly asked.
- Do not add Co-Authored-By or other AI-related Git trailers.
- Record every substantial AI-assisted change (date, goal, exact prompt, short summary, files changed):
  - project-wide work (setup, architecture, specs, cross-service changes) → root `PROMPT_HISTORY.md`
  - service-specific work → that service's `<service>/PROMPT_HISTORY.md`
