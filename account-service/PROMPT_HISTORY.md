# Prompt History – account-service

This file records AI-assisted development prompts that relate **only to `account-service`**.
Project-wide prompts (setup, architecture, specifications, cross-service changes) are recorded in the root [`PROMPT_HISTORY.md`](../PROMPT_HISTORY.md).

For every substantial AI-assisted change to this service, append an entry with: date, goal, exact user prompt, short summary of changes, and files created/modified.

---

## Prompt 1

**Date:** 2026-10-08

**Goal:** Create the account-service skeleton (Maven module, configuration, main class, layer packages, startup test) without any account features.

**Exact user prompt:**

````text
Lets first create skeleton of account service, put it into root Maven modules, application.yml, main class, layers and startup test.

Follow CLAUDE.md and relevant specs.
Use the existing catalog-service as reference where useful.
Run the relevant tests after changes.
Update account-service/PROMPT_HISTORY.md with this prompt and short summary.

Do not implement account features yet, just the skeleton.
Do not commit or push.
````

**Summary of changes:**

- Added `account-service` Maven module mirroring `catalog-service` (Spring Web, Spring Data JPA, H2, Spring Boot Test); registered it in the root `pom.xml` `<modules>`.
- `application.yml`: port 8084, context path `/api`, H2 in-memory database `accountdb`, H2 console enabled (as in catalog-service).
- `AccountServiceApplication` main class in package `com.csci318.accountservice`.
- Layer packages `presentation`, `service`, `domain`, `infrastructure` with `package-info.java` descriptions.
- `AccountServiceApplicationTests` context-load test (passes).
- No domain classes, endpoints, events or business logic added.

**Files created/modified:**

- `pom.xml` (root – added `<module>account-service</module>`)
- `account-service/pom.xml`
- `account-service/src/main/resources/application.yml`
- `account-service/src/main/java/com/csci318/accountservice/AccountServiceApplication.java`
- `account-service/src/main/java/com/csci318/accountservice/{presentation,service,domain,infrastructure}/package-info.java`
- `account-service/src/test/java/com/csci318/accountservice/AccountServiceApplicationTests.java`
- `account-service/PROMPT_HISTORY.md`

---

## Prompt 2

**Date:** 2026-10-08

**Goal:** Implement A1 user registration (`POST /users`) and user lookup (`GET /users/{userId}`) with BCrypt password hashing, basic 400/404 handling and tests.

**Exact user prompt:**

````text
Lets move to A1 now.

Implement basic user registration and user lookup:
- User aggregate with UserProfile
- repository and service
- POST /users
- GET /users/{userId}
- use BCrypt for password hashing with spring-security-crypto
- add basic 400/404 handling and tests

Keep UserRegistered and UserProfileUpdated as internal domain events for now, not Kafka.

Also update account-service README and root README so they no longer say account-service is a placeholder.

Follow CLAUDE.md and specs.
Run relevant tests.
Update account-service/PROMPT_HISTORY.md for the service work and root PROMPT_HISTORY.md only for the root README change.
Do not commit or push.
````

**Summary of changes:**

- Added `spring-security-crypto` (BCrypt only; Spring Security itself is not used) to `account-service/pom.xml`.
- Domain: `User` aggregate root (id, username unique, email, phone, passwordHash; table `users`) with embedded `UserProfile` value object (firstName, lastName). `User.register(...)` checks required fields and registers the internal `UserRegistered` domain event (published in-process by Spring Data `AbstractAggregateRoot` on save, not Kafka). `ValidationException` for invariant violations.
- Infrastructure: `UserRepository` (Spring Data JPA), `PasswordEncoderConfig` (`BCryptPasswordEncoder` bean).
- Service: `UserService` (register: password required, username must be unique, password hashed with BCrypt; lookup: 404 if missing), `RegisterUserCommand`, `ResourceNotFoundException`.
- Presentation: `UserController` (`POST /users` → 201, `GET /users/{userId}` → 200), `UserRegistrationRequest`, `UserResponse` (never exposes the password hash), `ErrorResponse` (`code`, `message`), `GlobalExceptionHandler` (`ValidationFailed` 400, `ResourceNotFound` 404).
- Tests: `UserTest` (domain), `UserControllerTest` (MockMvc: 201 create, BCrypt hash stored, `UserRegistered` published, 200 lookup, 404 unknown user, 400 missing field / missing password / duplicate username / malformed body).
- `account-service/README.md` updated (no longer a placeholder; lists implemented endpoints).
- `UserProfileUpdated` not added yet: it belongs to `PUT /users/{userId}/profile`, which was not part of this task.

**Files created/modified:**

- `account-service/pom.xml`
- `account-service/README.md`
- `account-service/src/main/java/com/csci318/accountservice/domain/User.java`
- `account-service/src/main/java/com/csci318/accountservice/domain/UserProfile.java`
- `account-service/src/main/java/com/csci318/accountservice/domain/UserRegistered.java`
- `account-service/src/main/java/com/csci318/accountservice/domain/ValidationException.java`
- `account-service/src/main/java/com/csci318/accountservice/infrastructure/UserRepository.java`
- `account-service/src/main/java/com/csci318/accountservice/infrastructure/PasswordEncoderConfig.java`
- `account-service/src/main/java/com/csci318/accountservice/service/UserService.java`
- `account-service/src/main/java/com/csci318/accountservice/service/RegisterUserCommand.java`
- `account-service/src/main/java/com/csci318/accountservice/service/ResourceNotFoundException.java`
- `account-service/src/main/java/com/csci318/accountservice/presentation/UserController.java`
- `account-service/src/main/java/com/csci318/accountservice/presentation/UserRegistrationRequest.java`
- `account-service/src/main/java/com/csci318/accountservice/presentation/UserResponse.java`
- `account-service/src/main/java/com/csci318/accountservice/presentation/ErrorResponse.java`
- `account-service/src/main/java/com/csci318/accountservice/presentation/GlobalExceptionHandler.java`
- `account-service/src/test/java/com/csci318/accountservice/domain/UserTest.java`
- `account-service/src/test/java/com/csci318/accountservice/presentation/UserControllerTest.java`
- `account-service/PROMPT_HISTORY.md`

---

## Prompt 3

**Date:** 2026-10-12

**Goal:** Review all uncommitted account-service work (skeleton + first part of A1) against the specs and CLAUDE.md, run the tests, and check nothing unrelated was changed. No new features.

**Exact user prompt:**

````text
First read CLAUDE.md, relevant account-service specs and account-service/PROMPT_HISTORY.md.

We already implemented the account-service skeleton and the first part of A1: user registration and user lookup.

Before we continue, check all current uncommitted changes and make sure everything we did so far matches the specs and project rules.

Run account-service tests and the full Maven tests.
Also check that we didn't accidentally change anything unrelated.

Don't implement any new features yet.
If you find an important problem, tell me before changing it. Small obvious fixes are fine.

Update account-service/PROMPT_HISTORY.md with this prompt and the result.
Do not commit or push.
````

**Summary of result (review only, no code changes):**

- Uncommitted changes are limited to: root `pom.xml` (one `<module>account-service</module>` line), root `README.md` (account-service status), root `PROMPT_HISTORY.md` (Prompt 12), `account-service/{README.md,PROMPT_HISTORY.md}` and the new `account-service/pom.xml` and `account-service/src/`. Nothing in `specs/`, `source-docs/`, `CLAUDE.md`, `catalog-service` or the other service folders was changed. `git diff --check` reports no whitespace errors.
- Matches the specs: port 8084, base path `/api`; `POST /users` → 201 `UserResponse`, `GET /users/{userId}` → 200 `UserResponse` (api-endpoints.md A1); `User` aggregate root with `id`, `username` (unique), `email`, `phone`, `passwordHash` and `UserProfile` value object with optional `firstName`/`lastName` (domain-model.md 4.1–4.2); errors use the `ValidationFailed` (400) and `ResourceNotFound` (404) codes from the error contract; `UserRegistered` is internal only, which matches the specs (it is not listed in the Kafka flows in architecture.md).
- Matches project rules: layered packages `presentation`/`service`/`domain`/`infrastructure`; only extra library is `spring-security-crypto`, as requested (version managed by the Spring Boot parent); configuration mirrors catalog-service; prompt history recorded in both files.
- Tests: `./mvnw test -pl account-service` → 11 tests, 0 failures (`AccountServiceApplicationTests` 1, `UserTest` 2, `UserControllerTest` 8). `./mvnw test` (full build: catalog-service + account-service) → BUILD SUCCESS, catalog-service 1 test, account-service 11 tests, 0 failures.
- Known gaps / notes (not fixed, no important problem found):
  - `GET /users/{userId}` is not protected yet; the specs require authentication for account-specific endpoints (NFR-04, A2). Planned for A2.
  - A duplicate username returns 400 `ValidationFailed`; the error contract has no account-specific 409 code, so this is an assumption.
  - Two simultaneous registrations with the same username can pass the `existsByUsername` check together; the DB unique constraint then rejects one with a default Spring 500 response, not the error contract format. There is no catch-all `InternalServiceError` (500) handler yet.
  - `User` uses an assigned UUID id, so Spring Data `save` uses `merge` (one extra SELECT before the INSERT). It works correctly; this only costs one extra query.
  - The remaining A1 endpoints (`PUT /users/{userId}/profile` with `UserProfileUpdated`, and the address endpoints) are not implemented yet.

**Files created/modified:**

- `account-service/PROMPT_HISTORY.md` (this entry only)

---

## Prompt 4

**Date:** 2026-10-12

**Goal:** Fix issue 3 from the Prompt 3 review: handle database unique-username violations and unexpected internal errors so responses match the API error contract.

**Exact user prompt:**

````text
Fix issue 3 before we continue A1. Add proper handling for database unique username violations and unexpected internal errors so responses match the API error contract. Add tests, update account-service prompt history, and don’t change anything else.
````

**Summary of changes:**

- `UserService.registerUser` now uses `saveAndFlush`, so a concurrent registration with the same username fails inside the service on the `users.username` unique constraint. The resulting `DataIntegrityViolationException` is converted to `ValidationException("username is already taken")` → 400 `ValidationFailed`, the same response as the existing pre-check. No `UserRegistered` event is published for the rejected registration.
- `GlobalExceptionHandler`: new catch-all handler → 500 `InternalServiceError` with a generic message; the exception is logged, not returned. Standard Spring MVC exceptions (those implementing `org.springframework.web.ErrorResponse`, e.g. unknown path 404, 405, 415) are rethrown so Spring keeps their normal status instead of turning them into 500.
- Tests:
  - `DuplicateUsernameConstraintTest`: `@SpyBean UserRepository` makes `existsByUsername` return false (simulating the race), so the second registration hits the DB constraint → 400 `ValidationFailed` "username is already taken", and only one `UserRegistered` event is published.
  - `GlobalExceptionHandlerTest`: `@MockBean UserRepository` throws on `findById` → 500 `InternalServiceError` without leaking the exception message; unknown path still 404, unsupported method still 405.
- Test results: `./mvnw test -pl account-service` → 15 tests, 0 failures; `./mvnw test` (full build) → BUILD SUCCESS.
- Environment note: the local JDK is Java 26 (project targets Java 21). Mockito's bundled Byte Buddy cannot mock concrete classes on Java 26 (`@MockBean UserService` failed to load the context), so the tests mock the `UserRepository` interface instead. No build configuration was changed.

**Files created/modified:**

- `account-service/src/main/java/com/csci318/accountservice/service/UserService.java`
- `account-service/src/main/java/com/csci318/accountservice/presentation/GlobalExceptionHandler.java`
- `account-service/src/test/java/com/csci318/accountservice/presentation/DuplicateUsernameConstraintTest.java` (new)
- `account-service/src/test/java/com/csci318/accountservice/presentation/GlobalExceptionHandlerTest.java` (new)
- `account-service/PROMPT_HISTORY.md`
