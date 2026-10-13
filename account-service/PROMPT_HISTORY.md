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

---

## Prompt 5

**Date:** 2026-10-12

**Goal:** Review the current state of A1 (Register and Manage Account) in account-service against the specs and list what is still needed to complete it. Review only — no implementation or spec changes.

**Exact user prompt:**

````text
Firstly read project rules, CLAUDE.md, relevant Account specs, account-service/PROMPT_HISTORY.md, look at the current state of account-service, check what is already done with part A1 and what needed to be done to complete it. Don't make any changes to the implementation or specs. Only append this prompt and your review result to account-service/PROMPT_HISTORY.md
````

**Specs read:** `user-stories.md` (A1 acceptance rules, story mapping, endpoint mapping), `api-endpoints.md` (Account Service endpoints, naming rules, error contract), `domain-model.md` (4.1 User, 4.2 UserProfile, 4.4 Address), `requirements.md` (FR-10, FR-15, FR-16, NFR-04), `architecture.md` (Account Service, REST callers, Kafka flows).

**State at review time:** branch `feature/account-service`, working tree clean, everything up to Prompt 4 is committed (`849e262`). `./mvnw test -pl account-service` → 15 tests, 0 failures (`AccountServiceApplicationTests` 1, `UserTest` 2, `UserControllerTest` 8, `DuplicateUsernameConstraintTest` 1, `GlobalExceptionHandlerTest` 3). The ERROR log lines during the run are expected (deliberate unique-constraint violation and deliberate 500 in the tests).

**A1 – already done (acceptance rule 1, and the "view" half of rule 2):**

| Spec item | Status |
|---|---|
| `POST /users` → 201 `UserResponse` | Done (`UserController.register`) |
| `GET /users/{userId}` → 200 `UserResponse` | Done (`UserController.getUser`), 404 `ResourceNotFound` for unknown user |
| `User` aggregate root (id, username unique, email, phone, passwordHash) | Done; password hashed with BCrypt, never returned |
| `UserProfile` value object (firstName, lastName, optional) | Done (`@Embeddable` record) |
| `UserRegistered` domain event | Done; internal only (Spring `AbstractAggregateRoot`), not Kafka — matches architecture.md (not a Kafka flow) |
| Error contract | `ValidationFailed` 400, `ResourceNotFound` 404, `InternalServiceError` 500 (catch-all, Prompt 4); duplicate username incl. DB-constraint race → 400 (Prompt 4) |

**A1 – still to do (acceptance rules 2 "update profile" and 3 "addresses"):**

1. **Update profile — `PUT /users/{userId}/profile`** (`ProfileUpdateRequest` → 200 `UserResponse`).
   - Domain: a `User.updateProfile(UserProfile)` behaviour on the aggregate that registers a new internal `UserProfileUpdated` domain event (listed for A1 in user-stories.md; keep it internal like `UserRegistered`, it is not a Kafka flow).
   - Service: `UserService.updateProfile(...)`; 404 for unknown user. The aggregate must be saved through `userRepository.save(...)` (dirty checking alone does not publish `AbstractAggregateRoot` events).
   - Presentation: `ProfileUpdateRequest`, controller method.
2. **Address entity (domain-model.md 4.4):** `Address` entity with `id`, `streetNo` (Integer), `streetName`, `suburb`, `city`, `state`, `postCode` (Integer); all fields required. Relationship User 1 — 0..* Address. Since Address is an Entity (not an aggregate root), it belongs to the `User` aggregate and should be added/updated/removed through `User` methods (e.g. `@OneToMany(cascade = ALL, orphanRemoval = true)`), not through a separate aggregate.
3. **Address endpoints (api-endpoints.md):**
   - `GET /users/{userId}/addresses` → 200 `Array<AddressResponse>`
   - `POST /users/{userId}/addresses` (`AddressCreateRequest`) → 201 `AddressResponse`
   - `PUT /users/{userId}/addresses/{addressId}` (`AddressUpdateRequest`) → 200 `AddressResponse`
   - `DELETE /users/{userId}/addresses/{addressId}` → 204 No Content
   - 404 `ResourceNotFound` for an unknown user, or an address that does not exist / belongs to another user; 400 `ValidationFailed` for missing fields or non-integer `streetNo`/`postCode` (malformed JSON is already handled).
   - `AddressResponse` must include `id`: Order Service stores it as `Order.deliveryAddressId` (O2, FR-10).
   - No address domain events: the specs list only `UserRegistered` and `UserProfileUpdated` for A1.
4. **Tests:** domain tests for `updateProfile` and address add/update/remove; MockMvc tests for the new endpoints (success codes, 400, 404, `UserProfileUpdated` published, address of another user → 404).
5. **Docs:** update `account-service/README.md` "Implemented" table and the root `README.md` account-service status when A1 is complete (root change recorded in root `PROMPT_HISTORY.md`).

**Open questions / assumptions to confirm before implementing (specs do not define these):**

- The fields of `ProfileUpdateRequest`, `AddressCreateRequest`, `AddressUpdateRequest`, `AddressResponse` and `UserResponse` are not defined in the specs. Proposed: `ProfileUpdateRequest` = `firstName`, `lastName` only (the `UserProfile` value object); email/phone/password are not updated by this endpoint. Address requests/response mirror the domain-model 4.4 fields (+ `id` in the response).
- PUT semantics: proposed full replacement (PUT profile with a missing field clears it, since both are optional; PUT address requires all fields).
- Spec issue (report only, not changed): `postCode` is `Integer` in domain-model.md 4.4, so postcodes with a leading zero (e.g. NT `0800`) would lose it. `String` would be safer; needs a spec decision by the team.
- Whether `UserProfileUpdated` should still be raised when the new profile equals the old one (proposed: always raise on a successful update, simplest).

**Out of scope for A1 (not to be implemented now):**

- Authentication / protecting `GET /users/{userId}`, profile and address endpoints (NFR-04, A2 `POST /auth/login`). When A2 is added, `POST /users` must stay public.
- Preferences (A3 `GET`/`PUT /users/{userId}/preferences`, `PreferenceUpdated`).
- Kafka publishing of account events (no account Kafka flow in architecture.md).

**Still-open notes from Prompt 3:** duplicate username → 400 `ValidationFailed` (no account-specific 409 code in the contract — assumption); assigned UUID id causes one extra SELECT on `save` (harmless).

**Files created/modified:**

- `account-service/PROMPT_HISTORY.md` (this entry only)

---

## Prompt 6

**Date:** 2026-10-12

**Goal:** Finish A1: profile update with `UserProfileUpdated`, address request/response and full address CRUD.

**Exact user prompt:**

````text
Finish A1 only: profile update, address request and response, full CRUD for addresses, and UserProfileUpdated.
Use the existing specs and current account-service structure.
Keep postCode as Integer for now. UserProfileUpdated should only be raised when the profile actually changes.
Don't work on A2, A3 or Kafka.
Run account-service tests and the full Maven tests. Update the account-service README and prompt history as needed.
Don't commit or push.
````

**Summary of changes:**

- Domain:
  - `Address` entity (domain-model.md 4.4: `id`, `streetNo` Integer, `streetName`, `suburb`, `city`, `state`, `postCode` Integer; all required → `ValidationException`). Table `addresses`. Its constructor and `update` are package-private, so addresses can only be changed through `User`. An update is validated before any field changes.
  - `User` aggregate: `@OneToMany(cascade = ALL, orphanRemoval = true)` addresses via `user_id` join column (`updatable = false` avoids Hibernate's extra `UPDATE` after each insert). New methods: `updateProfile`, `addAddress`, `updateAddress` (returns `Optional`, empty if the address is not this user's), `removeAddress` (returns `false` if not found), `findAddress`, `getAddresses` (read-only list).
  - `updateProfile` replaces the whole profile and registers the internal `UserProfileUpdated(userId, firstName, lastName, occurredAt)` event **only when the profile actually changes**. A missing profile (Hibernate loads all-null embedded columns as `null`) is treated as an empty profile.
- Service: `UserService.updateProfile` (calls `save` so the domain event is published), `getAddresses`, `addAddress`, `updateAddress`, `removeAddress`; new `UpdateProfileCommand` and `AddressCommand`. 404 `ResourceNotFound` for an unknown user or an address that is missing or belongs to another user.
- Presentation:
  - `UserController`: `PUT /users/{userId}/profile` → 200 `UserResponse`.
  - New `AddressController` (`/users/{userId}/addresses`): `GET` → 200 array, `POST` → 201, `PUT /{addressId}` → 200, `DELETE /{addressId}` → 204.
  - New `ProfileUpdateRequest`, `AddressCreateRequest`, `AddressUpdateRequest`, `AddressResponse` (includes `id`, used by Order Service as `deliveryAddressId`).
- Tests:
  - `UserTest` (+4): profile replace, address add/update/remove incl. unknown id, required address fields, failed update leaves the address unchanged.
  - `UserControllerTest` (+5): profile update → 200 and one `UserProfileUpdated`; partial body clears the missing field; unchanged profile → no event; empty profile updated with `{}` → no event; unknown user → 404.
  - `AddressControllerTest` (new, 11): create, list from the database (flush + clear), empty list, update, delete → 204, missing field → 400, non-integer `postCode` → 400, update with missing field → 400, unknown user → 404, unknown address → 404, another user's address → 404 and unchanged.
- `account-service/README.md`: Implemented table lists all A1 endpoints; notes on profile/address semantics, events and errors.
- Test results: `./mvnw test -pl account-service` → 35 tests, 0 failures. `./mvnw test` (full build) → BUILD SUCCESS (catalog-service 1, account-service 35).

**Assumptions (specs do not define these):**

- `ProfileUpdateRequest` = `firstName`, `lastName` only (the `UserProfile` value object); email, phone and password are not changed by it.
- PUT is full replacement: a missing profile field is cleared; an address update requires all fields.
- Address request/response fields mirror domain-model.md 4.4 (+ `id` in the response). `UserResponse` does not include addresses (they have their own endpoint).
- No address domain events (the specs list only `UserRegistered` and `UserProfileUpdated` for A1).

**Not done / open:**

- `postCode` kept as Integer as instructed (leading zeros such as `0800` are lost — spec issue noted in Prompt 5).
- A1 endpoints are not protected yet (NFR-04 / A2); A2, A3 and Kafka were out of scope.
- Root `README.md` still says account-service is "in progress (A1 registration + user lookup)"; not changed because this prompt only asked for the account-service README.

**Files created/modified:**

- `account-service/src/main/java/com/csci318/accountservice/domain/Address.java` (new)
- `account-service/src/main/java/com/csci318/accountservice/domain/UserProfileUpdated.java` (new)
- `account-service/src/main/java/com/csci318/accountservice/domain/User.java`
- `account-service/src/main/java/com/csci318/accountservice/service/UserService.java`
- `account-service/src/main/java/com/csci318/accountservice/service/UpdateProfileCommand.java` (new)
- `account-service/src/main/java/com/csci318/accountservice/service/AddressCommand.java` (new)
- `account-service/src/main/java/com/csci318/accountservice/presentation/UserController.java`
- `account-service/src/main/java/com/csci318/accountservice/presentation/AddressController.java` (new)
- `account-service/src/main/java/com/csci318/accountservice/presentation/ProfileUpdateRequest.java` (new)
- `account-service/src/main/java/com/csci318/accountservice/presentation/AddressCreateRequest.java` (new)
- `account-service/src/main/java/com/csci318/accountservice/presentation/AddressUpdateRequest.java` (new)
- `account-service/src/main/java/com/csci318/accountservice/presentation/AddressResponse.java` (new)
- `account-service/src/test/java/com/csci318/accountservice/domain/UserTest.java`
- `account-service/src/test/java/com/csci318/accountservice/presentation/UserControllerTest.java`
- `account-service/src/test/java/com/csci318/accountservice/presentation/AddressControllerTest.java` (new)
- `account-service/README.md`
- `account-service/PROMPT_HISTORY.md`

---

## Prompt 7

**Date:** 2026-10-12

**Goal:** Work out exactly what A2 (Authenticate Customer) requires, list everything the specs leave undefined, and list the decisions needed before implementation. Review only — no implementation or spec changes.

**Exact user prompt:**

````text
Read CLAUDE.md, the relevant Account specs, account-service/PROMPT_HISTORY.md, and the current state of the account-service.
Figure out exactly what A2 requires. Summarise everything that is not defined by the specification and list the decisions that need to be made before implementation.
Do not change any implementation or specs. Only append this review and the exact prompt to account-service/PROMPT_HISTORY.md.
````

**Specs read:** `user-stories.md` (A2 story, acceptance rules, story mapping, endpoint mapping), `api-endpoints.md` (Account Service endpoints, contract rules, error contract), `requirements.md` (FR-16, NFR-04), `domain-model.md` (4.1 User), `architecture.md` (REST callers, Kafka flows, Decision 11). `cqrs.md` and `event-sourcing.md` cover Inventory only and say nothing about Account or authentication.

**State at review time:** branch `feature/account-service`, working tree clean, A1 complete (Prompt 6). `./mvnw test -pl account-service` → 35 tests, 0 failures. Only `catalog-service` and `account-service` contain code; `inventory-service`, `order-service` and `recommendation-service` are still README/PROMPT_HISTORY placeholders.

### What the specs define for A2

| Source | Requirement |
|---|---|
| user-stories.md A2 | "As a Registered Customer, I want to log in to my account, so that I can securely access account-specific features." Acceptance rules: (1) registered customers can authenticate using their account credentials; (2) account-specific functionality requires authentication; (3) invalid authentication attempts do not grant access. |
| user-stories.md mapping | Domain class: `User` only. Domain events: **None**. Endpoint: `POST /auth/login`. |
| api-endpoints.md | `POST /auth/login` (Account Service, `http://localhost:8084/api`), body `LoginRequest` → `200 OK (AuthenticationResponse)`, "Authenticate a registered customer". Rule: "Account-specific and administrative endpoints require authentication and appropriate authorization." |
| api-endpoints.md error contract | `AuthenticationRequired` 401 (protected endpoints; authentication missing or invalid), `AccessDenied` 403 (authenticated user lacks permission). `ValidationFailed` 400, `InternalServiceError` 500 also apply. |
| requirements.md | FR-16: registered customers authenticate (log in) before accessing account-specific features. NFR-04: account data and administrative endpoints require authentication **and authorisation**. |
| domain-model.md 4.1 | `User.passwordHash` (String, required) is the credential "used for authentication (A2); the plain password is never stored". No role, token, session or login-attempt fields. |
| architecture.md Decision 11 (Open) | Admin authorisation has no mechanism yet ("no role on `User`, no token format for `AuthenticationResponse`"); to be decided project-wide together with A2. |

Already in place that A2 can reuse: BCrypt `PasswordEncoder` bean (`PasswordEncoderConfig`, `spring-security-crypto`), unique `username` column, `ErrorResponse` + `GlobalExceptionHandler` (no 401/403 handlers yet). Missing: `UserRepository.findByUsername`, any login endpoint, any request authentication.

### Not defined by the specification

1. **`LoginRequest` fields.** "Account credentials" is not specified. Only `username` is unique (`email` is not), so email login is not safe without a spec change.
2. **`AuthenticationResponse` fields and token format** (explicitly called undefined in Decision 11). Nothing says whether login returns a token at all, what kind (signed JWT, opaque random token, HTTP Basic, server session), or which fields (token, type, expiry, userId, user details).
3. **How the client proves authentication on later requests** (e.g. `Authorization: Bearer <token>` header, cookie, Basic auth on every call).
4. **Token lifetime / expiry, logout, revocation.** No expiry rule, and no logout endpoint exists in the specs.
5. **Where tokens or sessions are stored.** An opaque token needs server-side state, but the domain model has no class for it; a stateless signed token needs a signing secret and its configuration.
6. **Exactly which endpoints are "account-specific".** Not listed. Obvious candidates: `GET /users/{userId}`, `PUT /users/{userId}/profile`, all `/users/{userId}/addresses` endpoints, and later A3 `/users/{userId}/preferences`. Must be public: `POST /users`, `POST /auth/login`.
7. **What "appropriate authorization" means for customers.** Not stated whether a logged-in user may only access their own `/users/{userId}/...` resources (ownership check → 403 `AccessDenied`), or whether any authenticated user may.
8. **Error precedence:** for a request with a valid token to another (or non-existent) `userId`, whether to return 403 or 404.
9. **Login failure responses.** The error contract applies `AuthenticationRequired` to "protected endpoints"; it does not say what `POST /auth/login` returns for unknown username / wrong password (401?), whether the two cases get the same message, or whether missing fields are 400 `ValidationFailed` or 401.
10. **Service-to-service calls.** Order Service → Account Service (saved addresses for O2/FR-10, user checks for O5) and Recommendation Service → Account Service (preferences for A3/R1) are synchronous REST calls to account-specific data. Nothing says whether they forward the customer's token, use a separate service credential, or are exempt.
11. **Scope across services.** "Account-specific functionality requires authentication" could include other services' user endpoints (`GET /users/{userId}/orders`, `GET /users/{userId}/recommendations`, `POST /products/{productId}/reviews` with `userId` in the body). Not stated whether A2 covers only Account Service or defines a mechanism the other services must also verify.
12. **Administrator identity (Decision 11).** NFR-04 / C5 / I3 need administrators, but `User` has no role and there is no way to create an admin. Adding a role would be a domain-model change.
13. **Brute-force protection** (lockout, attempt limits) and username case-sensitivity at login — not mentioned.
14. **Library / framework choice.** CLAUDE.md allows extra libraries only when required. Spring Security (filter chain) vs a small `HandlerInterceptor`, and a JWT library (e.g. jjwt / Nimbus) vs an opaque token, are not decided anywhere.
15. **DTO definitions.** api-endpoints.md has a DTO table for Catalog only; there is no Account DTO table, so none of `LoginRequest`, `AuthenticationResponse` (or the A1 DTOs) are formally defined.

### Decisions needed before implementation (with a suggested default)

| # | Decision | Suggested default (simplest that meets the specs) |
|---|---|---|
| D1 | `LoginRequest` fields | `username`, `password` (username is the only unique credential). |
| D2 | Token type | Either (a) opaque random token stored in Account DB with expiry — no new library, but a new persisted class not in the domain model, and other services cannot verify it without a new endpoint (not in specs); or (b) HMAC-signed JWT with a shared secret — other services can verify it locally, but needs a JWT library or hand-written HMAC code. Recommend (b) if Order/Recommendation endpoints will also be protected, otherwise (a). **Team decision, project-wide (Decision 11).** |
| D3 | `AuthenticationResponse` fields | `token`, `tokenType` ("Bearer"), `expiresAt`, `userId` (the client needs `userId` for `/users/{userId}/...`). |
| D4 | Transport | `Authorization: Bearer <token>` header. |
| D5 | Expiry / logout | Fixed lifetime (e.g. 1 hour) from configuration; no logout or refresh endpoint (not in specs). |
| D6 | Protected endpoints in account-service | All `/users/{userId}/**` and `GET /users/{userId}`; public: `POST /users`, `POST /auth/login`, H2 console. |
| D7 | Customer authorisation | Ownership: token's userId must equal path `userId`, otherwise 403 `AccessDenied`; checked before loading the user (so another user's id gives 403, not 404). |
| D8 | Login failure | 401 `AuthenticationRequired` with one generic message for unknown username and wrong password; missing `username`/`password` → 400 `ValidationFailed`. Missing/invalid/expired token on a protected endpoint → 401 `AuthenticationRequired`. |
| D9 | Mechanism | `HandlerInterceptor` (or one servlet filter) in `presentation`/`infrastructure`, reusing the existing BCrypt encoder; no `spring-boot-starter-security` (avoids CSRF/default-login configuration). |
| D10 | Service-to-service calls | Calling service forwards the customer's bearer token; no separate service credentials. Can be implemented when Order/Recommendation exist, but D2 must allow it. |
| D11 | Other services' user endpoints | Out of scope for A2 implementation in account-service; record the decision so other services follow D2/D4 later. |
| D12 | Admin authorisation (Decision 11) | Keep deferred (still Open), or add a `role` field to `User` — requires a spec change to domain-model.md 4.1 and a way to create admins. Needs explicit team decision. |
| D13 | Domain events | None (spec says A2 has no events); no `UserLoggedIn`/`LoginFailed`. |
| D14 | Brute-force protection / case-sensitivity | None; username match is case-sensitive, as in registration. |

### Impact notes for implementation

- All existing A1 MockMvc tests that call `GET /users/{userId}`, profile or address endpoints will need a valid token (e.g. a test helper that registers and logs in), and new tests are needed for 401 (missing/invalid/expired token, wrong password, unknown user) and 403 (another user's id).
- `GlobalExceptionHandler` needs `AuthenticationRequired` (401) and `AccessDenied` (403) mappings.
- `UserRepository` needs `findByUsername`.
- Once D1–D3 are agreed, `specs/api-endpoints.md` should get an Account Service DTO table (`LoginRequest`, `AuthenticationResponse`) and Decision 11 should be updated — spec changes to be made only on request.

**Files created/modified:**

- `account-service/PROMPT_HISTORY.md` (this entry only)

---

## Prompt 8

**Date:** 2026-10-12

**Goal:** List the implementation plan for A2 based on the approved JWT/Spring Security design (Architecture Decision 11). Review only — no code or spec changes.

**Exact user prompt:**

````text
Read CLAUDE.md, the relevant project specs, and account-service/PROMPT_HISTORY.md. Then shortly list the current implementation plan for A2 based on the approved JWT/Spring Security design. Do not write code or change any implementation/spec files. Append this prompt and your review to account-service/PROMPT_HISTORY.md.
````

**Basis:** `specs/architecture.md` Decision 11 (Resolved), `specs/api-endpoints.md` (Account Service DTOs (A2), Authentication and Authorisation, error contract), `specs/domain-model.md` 4.1 (`User.role`), `specs/user-stories.md` A2 (no domain events). Spring Boot 3.3.2, Java 21. Working tree clean; A1 complete, 35 tests passing.

### Implementation plan (account-service only)

1. **Dependencies (`account-service/pom.xml`):** add `spring-boot-starter-security` and `spring-boot-starter-oauth2-resource-server`. The resource-server starter brings Nimbus JOSE, which can both encode and decode HS256 JWTs. The explicit `spring-security-crypto` dependency then comes transitively and can be removed. Optional test dependency: `spring-security-test`.
2. **Configuration (`application.yml`):**
   - `security.jwt.secret`: a development default of at least 256 bits; must be the same in all services.
   - Admin seed properties (username, password, email, phone) with development defaults.
   - The 1-hour token lifetime as a constant or property.
3. **Domain:**
   - `Role` enum (`CUSTOMER`, `ADMIN`).
   - `User.role` stored as a string column, `nullable = false`.
   - `User.register(...)` always sets `CUSTOMER`; a separate factory creates the admin account.
   - No new domain events (A2 has none); `UserRegistered` is unchanged.
4. **Infrastructure:**
   - `UserRepository.findByUsername`.
   - `SecurityConfig` (`SecurityFilterChain`):
     - stateless, no CSRF / form login / HTTP Basic;
     - public: `POST /users`, `POST /auth/login`, `/h2-console/**` (frame options `sameOrigin`); everything else authenticated;
     - `oauth2ResourceServer().jwt()` with an HS256 `NimbusJwtDecoder` using the shared secret, and a `JwtAuthenticationConverter` that maps the `role` claim to `ROLE_<role>`;
     - an owner rule on `/users/{userId}` and `/users/{userId}/**`: the token `sub` must equal the `{userId}` path variable. Checking this in the filter chain gives 403 before the user is looked up (spec: 403 not 404), and an ADMIN gets no bypass.
   - A `JwtEncoder` bean (`NimbusJwtEncoder` with the same secret).
   - An `AuthenticationEntryPoint` and an `AccessDeniedHandler` that write the error contract JSON (`AuthenticationRequired` 401 / `AccessDenied` 403).
   - An admin seeder (`ApplicationRunner`) that creates the ADMIN user from configuration if that username does not exist yet.
5. **Service:**
   - `AuthService.login`: blank username or password → `ValidationException` (400). Unknown username or failed `passwordEncoder.matches` → a new authentication-failed exception (401), with the same message for both.
   - On success, a token is issued with `sub` = user id, `role`, `iat`, `exp` (+1 h).
   - Returns the token, `expiresAt`, user id and role.
6. **Presentation:**
   - `AuthController` (`POST /auth/login` → 200).
   - `LoginRequest` (username, password) and `AuthenticationResponse` (token, tokenType `Bearer`, expiresAt, userId, role) records.
   - `GlobalExceptionHandler`: map the authentication-failed exception to 401 `AuthenticationRequired`. Its catch-all must not turn Spring Security exceptions into 500.
7. **Tests:**
   - Domain: a registered user is `CUSTOMER`; the admin factory gives `ADMIN`.
   - Login:
     - 200 with a decodable token (claims `sub`, `role`, about 1 h expiry);
     - wrong password and unknown user both give 401 with the same message;
     - missing fields → 400;
     - the seeded admin logs in with role `ADMIN`.
   - Security:
     - no token, malformed token, expired token or wrong signature → 401 with an error contract body;
     - another user's id (existing or not) → 403;
     - an ADMIN token on another user's data → 403;
     - `POST /users` stays public.
   - Existing A1 tests (`UserControllerTest`, `AddressControllerTest`, `GlobalExceptionHandlerTest`, `DuplicateUsernameConstraintTest`) send a valid Bearer token, either from a login helper or minted with the `JwtEncoder` bean.
8. **Docs:** update `account-service/README.md` (A2 endpoint, security notes), then the root `README.md` status, recorded in the root `PROMPT_HISTORY.md`.

### Points to watch

- `server.servlet.context-path: /api`: Spring Security matchers use paths *without* `/api` (e.g. `/auth/login`).
- `GlobalExceptionHandlerTest` uses `@MockBean UserRepository`. The admin seeder runs at startup against the mock, so it must not depend on `save`'s return value. `DuplicateUsernameConstraintTest` stubs `existsByUsername` to false, so the seeder must not use that stubbed method. The local Java 26 / Mockito limitation (only interfaces can be mocked) still applies.
- Do not expose `role` in `UserResponse`: the A1 DTOs are not defined in the specs, and the login response already carries it.

### Out of scope for this task

- Protecting endpoints in Catalog / Inventory / Order / Recommendation (each service adopts the same resource-server configuration in its own task). `catalog-service` C5 is still unprotected and `catalog-service/README.md` still describes Decision 11 as open.
- A3 preferences (they will fall under the same owner rule automatically).
- Refresh tokens, logout, lockout or rate limiting (not in the specs).

**Files created/modified:**

- `account-service/PROMPT_HISTORY.md` (this entry only)

---

## Prompt 9

**Date:** 2026-10-12

**Goal:** Implement A2 (Authenticate Customer) in account-service following the approved JWT/Spring Security design (Architecture Decision 11, Prompt 8 plan).

**Exact user prompt:**

````text
Implement A2 just in account service. Follow the plan JWT/Spring Security plan. Add Role, admin seed, login, JWT generation/validation, 401/403, own-user access. Secrets (че зе секреты)/admin data through config/env, not hardcode. H2 console should stay okay. Update A1 tests for auth + add A2 tests. Update account-service/README.md and account-service/PROMPT_HISTORY.md. Run account tests and whole Maven build. Don't touch A3 and other services. No commit and push.
````

**Summary of changes:**

- **Dependencies:** `spring-security-crypto` replaced by `spring-boot-starter-security` and `spring-boot-starter-oauth2-resource-server` (Nimbus JOSE for HS256 encode/decode; BCrypt is still included).
- **Configuration:**
  - `application.yml`: `security.jwt.secret: ${JWT_SECRET}` and `security.admin.password: ${ADMIN_PASSWORD}`, with **no defaults**. The service fails at startup if either is missing; the JWT secret must be at least 32 bytes.
  - `security.admin.username` / `email` / `phone` come from `ADMIN_USERNAME` / `ADMIN_EMAIL` / `ADMIN_PHONE`, with non-secret defaults.
  - Test-only values in `src/test/resources/application-test.yml` (`test` profile).
- **Domain:** `Role` enum (`CUSTOMER`, `ADMIN`). `User.role` is stored as a string, not null. `User.register` always creates a `CUSTOMER`; the new `User.registerAdmin` creates an `ADMIN`. Both raise `UserRegistered` as before. No new domain events.
- **Infrastructure:**
  - `UserRepository.findByUsername`.
  - `JwtConfig`: HS256 secret key, `NimbusJwtEncoder` and `NimbusJwtDecoder`.
  - `JwtTokenService`: issues tokens with `sub`, `role`, `iat`, `exp` = +1 h.
  - `SecurityConfig`:
    - stateless; CSRF, form login and HTTP Basic disabled; frame options `sameOrigin` for the H2 console;
    - public: the ERROR dispatch, the H2 console, `POST /users`, `POST /auth/login`;
    - `/users/{userId}/**` (also matches `/users/{userId}`) only when the token's `sub` equals `{userId}`; anything else needs authentication;
    - the `role` claim is mapped to `ROLE_*`.
  - `AdminAccountInitializer` (`ApplicationRunner`): creates the admin from configuration if the username does not exist (logs a warning if it belongs to a non-admin).
  - `PasswordEncoderConfig`: javadoc updated.
- **Service:** `AuthService.login`:
  - blank username or password → 400 `ValidationFailed`;
  - unknown user or wrong password → new `AuthenticationFailedException`, with the same message ("Invalid username or password");
  - returns an `AuthenticationResult`.
- **Presentation:**
  - `AuthController` (`POST /auth/login` → 200); `LoginRequest`; `AuthenticationResponse` (token, tokenType `Bearer`, expiresAt, userId, role).
  - `SecurityErrorHandler`: entry point and access-denied handler that write error-contract JSON — 401 `AuthenticationRequired` with `WWW-Authenticate: Bearer`, 403 `AccessDenied`.
  - `GlobalExceptionHandler`: `AuthenticationFailedException` → 401 `AuthenticationRequired`. The catch-all rethrows Spring Security exceptions instead of returning 500.
- **Tests (56, all passing):**
  - Existing A1 tests: `@ActiveProfiles("test")`, and requests to `/users/{userId}/...` send a Bearer token for that user (`JwtTokenService`). The unknown-user 404 tests use a token for the unknown id.
  - `UserTest` (+2): roles.
  - `JwtConfigTest` (new, 2): secret length check.
  - `AuthControllerTest` (new, 8):
    - login → 200 with a token whose `sub`, `role` and 1 h expiry are checked by decoding, and `expiresAt` equals `exp`;
    - the login token opens the user's own account;
    - wrong password and unknown username → 401 with the same message;
    - missing, blank or malformed credentials → 400;
    - the seeded admin logs in with `ADMIN`; the admin is not created twice;
    - a `role` sent to `POST /users` is ignored (`CUSTOMER`).
  - `AccessControlTest` (new, 9):
    - all six A1 endpoints without a token → 401 with error body and `WWW-Authenticate`;
    - malformed, expired or foreign-signed token, or Basic auth → 401;
    - another user's endpoints → 403, and the data is unchanged;
    - another user's token on an unknown id → 403, not 404;
    - an ADMIN token does not bypass the own-user check;
    - registration and login stay public.
- **Manual check with the packaged jar:**
  - Without `JWT_SECRET` the service refuses to start ("Could not resolve placeholder 'JWT_SECRET'").
  - With env vars: admin created at startup; `/api/h2-console/` → 200 with `X-Frame-Options: SAMEORIGIN` and no token needed.
  - Login, own access 200, no token 401, other user 403; unknown path with a token → 404, `DELETE /users/{id}` → 405 (the ERROR dispatch still works).
- **Test results:**
  - `./mvnw test -pl account-service` → 56 tests, 0 failures.
  - `./mvnw test` (full build) → BUILD SUCCESS (catalog-service 46, account-service 56).
  - The ERROR log line during the run comes from the deliberate 500 test.
- `account-service/README.md`: A2 endpoint, authentication rules, environment variable table, run command with env vars.

**Assumptions:**

- No defaults for secrets (JWT secret, admin password); non-secret admin fields have defaults. The secret is used as raw UTF-8 bytes (at least 32).
- A request without a token to any non-public path (including unknown paths) returns 401 rather than 404.
- `role` is not added to `UserResponse` (the A1 DTOs are not defined in the specs; the login response carries it).
- Only one token lifetime (1 hour, a constant in `JwtTokenService`); the spec fixes it.

**Not done / open:**

- Other services do not validate tokens yet (catalog-service C5 is still unprotected; `catalog-service/README.md` still calls Decision 11 open). Each needs the same resource-server configuration and the same `JWT_SECRET`.
- Root `README.md` still lists account-service as "A1 complete; A2, A3 pending" (root change, not requested here).
- Login timing differs slightly between an unknown username (no BCrypt check) and a wrong password; not addressed (no requirement).
- A3 preferences not implemented; they will fall under the same `/users/{userId}/**` rule.

**Files created/modified:**

- `account-service/pom.xml`
- `account-service/src/main/resources/application.yml`
- `account-service/src/main/java/com/csci318/accountservice/domain/Role.java` (new)
- `account-service/src/main/java/com/csci318/accountservice/domain/User.java`
- `account-service/src/main/java/com/csci318/accountservice/infrastructure/UserRepository.java`
- `account-service/src/main/java/com/csci318/accountservice/infrastructure/PasswordEncoderConfig.java`
- `account-service/src/main/java/com/csci318/accountservice/infrastructure/JwtConfig.java` (new)
- `account-service/src/main/java/com/csci318/accountservice/infrastructure/JwtTokenService.java` (new)
- `account-service/src/main/java/com/csci318/accountservice/infrastructure/SecurityConfig.java` (new)
- `account-service/src/main/java/com/csci318/accountservice/infrastructure/AdminAccountInitializer.java` (new)
- `account-service/src/main/java/com/csci318/accountservice/service/AuthService.java` (new)
- `account-service/src/main/java/com/csci318/accountservice/service/AuthenticationResult.java` (new)
- `account-service/src/main/java/com/csci318/accountservice/service/AuthenticationFailedException.java` (new)
- `account-service/src/main/java/com/csci318/accountservice/presentation/AuthController.java` (new)
- `account-service/src/main/java/com/csci318/accountservice/presentation/LoginRequest.java` (new)
- `account-service/src/main/java/com/csci318/accountservice/presentation/AuthenticationResponse.java` (new)
- `account-service/src/main/java/com/csci318/accountservice/presentation/SecurityErrorHandler.java` (new)
- `account-service/src/main/java/com/csci318/accountservice/presentation/GlobalExceptionHandler.java`
- `account-service/src/test/resources/application-test.yml` (new)
- `account-service/src/test/java/com/csci318/accountservice/AccountServiceApplicationTests.java`
- `account-service/src/test/java/com/csci318/accountservice/domain/UserTest.java`
- `account-service/src/test/java/com/csci318/accountservice/infrastructure/JwtConfigTest.java` (new)
- `account-service/src/test/java/com/csci318/accountservice/presentation/UserControllerTest.java`
- `account-service/src/test/java/com/csci318/accountservice/presentation/AddressControllerTest.java`
- `account-service/src/test/java/com/csci318/accountservice/presentation/GlobalExceptionHandlerTest.java`
- `account-service/src/test/java/com/csci318/accountservice/presentation/DuplicateUsernameConstraintTest.java`
- `account-service/src/test/java/com/csci318/accountservice/presentation/AuthControllerTest.java` (new)
- `account-service/src/test/java/com/csci318/accountservice/presentation/AccessControlTest.java` (new)
- `account-service/README.md`
- `account-service/PROMPT_HISTORY.md`

---

## Prompt 10

**Date:** 2026-10-13

**Goal:** List the next steps for A3 (Save Recommendation Preferences) and point out everything the specs leave undefined that needs a decision before implementation. Review only — no implementation or spec changes.

**Exact user prompt:**

````text
First read CLAUDE.md, the relevant project specs, account-service/README.md and account-service/PROMPT_HISTORY.md. Then list the next steps for A3 implementation and point out anything that is not defined by the specs and needs a decision before implementation. Do not change any implementation or spec files. Only append this prompt and your review to account-service/PROMPT_HISTORY.md.
````

**Specs read:** `user-stories.md` (A3 story and scenario, story mapping, endpoint mapping), `api-endpoints.md` (Account Service endpoints, Authentication and Authorisation, error contract), `domain-model.md` (class table, 4.1 User, 4.3 Preference, 5.1 BuildRequest), `requirements.md` (FR-17, NFR-03, NFR-04), `architecture.md` (REST calls, Kafka flows, Decision 11). `cqrs.md` and `event-sourcing.md` cover Inventory only.

**State at review time:** branch `feature/account-service`, working tree clean, A1 and A2 complete and committed (`559f417`). `./mvnw test -pl account-service` → 56 tests, 0 failures. `recommendation-service` is still a README/PROMPT_HISTORY placeholder.

### What the specs define for A3

| Source | Requirement |
|---|---|
| user-stories.md A3 | "As a Customer, I want to save my PC preferences, so that they can be used when generating personalised PC build recommendations." Scenario: (1) customer enters or updates preferences; (2) Account Service stores them; (3) Recommendation Service can retrieve them when generating a build. |
| user-stories.md mapping | Domain classes: `User`, `Preference`. Domain event: `PreferenceUpdated`. Endpoints: `GET`, `PUT /users/{userId}/preferences`. |
| api-endpoints.md | `GET /users/{userId}/preferences` → 200 `PreferenceResponse`; `PUT /users/{userId}/preferences` (`PreferenceUpdateRequest`) → 200 `PreferenceResponse` ("Save or update"). |
| api-endpoints.md auth | Both endpoints are Customer (owner): token `sub` must equal `{userId}`, checked before lookup (403, not 404); ADMIN does not bypass. Recommendation → Account calls for preferences forward the customer's `Authorization` header. |
| domain-model.md 4.3 | `Preference` is a **Value Object** with one field: `preference` (String, Optional). Association User 1 — 0..1 Preference. |
| requirements.md | FR-17 (Account, Recommendation): customers save preferences used by the recommendation service. |
| architecture.md | Recommendation Service → Account Service is a synchronous REST call. `PreferenceUpdated` is not in the Kafka flows, so it is an internal domain event (like `UserRegistered` / `UserProfileUpdated`). |

Already in place: `SecurityConfig`'s `/users/{userId}/**` owner rule already covers `/users/{userId}/preferences` (no security change needed); `User` aggregate with `AbstractAggregateRoot` events; `UserService` / `UserController` patterns from A1; error contract handlers.

### Next steps (account-service only)

1. **Domain:**
   - `Preference` value object: `@Embeddable` record with one field `preference` (column `preference` on `users`), like `UserProfile`.
   - `User.preference` field and `User.updatePreference(Preference)`; raises the new internal `PreferenceUpdated` domain event (payload e.g. `userId`, `preference`, `occurredAt`).
   - Handle Hibernate loading an all-null embeddable as `null` (same as `getProfileOrEmpty`).
2. **Service:** `UserService.getPreference(userId)` and `updatePreference(...)` (or an `UpdatePreferenceCommand`, following `UpdateProfileCommand`); 404 `ResourceNotFound` for an unknown user; call `userRepository.save(...)` so the event is published.
3. **Presentation:** `PreferenceUpdateRequest`, `PreferenceResponse`, and `GET` / `PUT /users/{userId}/preferences` (in `UserController` or a small `PreferenceController`, like `AddressController`).
4. **Tests:**
   - `UserTest`: set, replace, clear; event raised / not raised.
   - MockMvc (new `PreferenceControllerTest`): GET before anything is saved, PUT → 200 and GET returns the saved value (after flush + clear), `PreferenceUpdated` published once, unknown user → 404 (token for the unknown id), malformed body → 400, plus whatever the decisions below require (too long, blank).
   - `AccessControlTest`: both endpoints without token → 401; another user's id → 403 and the preference unchanged; ADMIN token on another user → 403.
5. **Docs:** `account-service/README.md` (A3 rows in the Implemented table, semantics, remove "Not yet implemented: preferences"); root `README.md` status → A1–A3 complete (recorded in root `PROMPT_HISTORY.md`); this file.
6. **Spec (only if the team agrees, as a separate request):** add an "Account Service DTOs (A3)" table to `api-endpoints.md`, as was done for A2, once the decisions below are made.

### Not defined by the specs — decisions needed before implementation

| # | Open point | Suggested default |
|---|---|---|
| D1 | **`PreferenceUpdateRequest` / `PreferenceResponse` fields.** No Account DTO table for A3 (only A2 has one). Naming also differs: domain field `preference` (singular), endpoint `/preferences`, `BuildRequest.preferences`. | Request `{ "preference": String }`; response `{ "userId", "preference" }`. Use the domain-model name `preference`. |
| D2 | **Shape of the preference.** Spec says one free-text String; "PC preferences" could suggest structured fields (brands, budget, use). | Keep one free-text String as in domain-model 4.3; structured fields would be a spec change. |
| D3 | **Maximum length.** Not defined. JPA's default `varchar(255)` would make a longer value fail in the database, and the current catch-all would then return 500 instead of 400. | Pick a limit (e.g. 1000 characters) as `@Column(length)` and validate it in the domain → 400 `ValidationFailed`. |
| D4 | **`GET` when nothing has been saved yet** (User 1 — 0..1 Preference). 200 with an empty value, or 404? | 200 with `preference: null` for an existing user; 404 only for an unknown user. Simpler for Recommendation Service. |
| D5 | **`PUT` with a missing / null / blank `preference`.** Field is Optional and there is no `DELETE` endpoint. | Full replacement like `PUT /profile`: missing or null clears it; blank treated as cleared (stored as null). Alternatively reject blank with 400. |
| D6 | **When `PreferenceUpdated` is raised and its payload.** Spec only names the event. | Raise only when the value actually changes (same rule as `UserProfileUpdated`, Prompt 6); payload `userId`, `preference`, `occurredAt`; internal only, not Kafka. |
| D7 | **Should `UserResponse` include the preference?** Not defined. | No — it has its own endpoint (same as addresses). |
| D8 | **How Recommendation Service uses it** (R1): whether the saved preference is combined with or replaced by `BuildRequest.preferences` from the request body, and what happens if Account Service is unavailable (NFR-03). | Out of scope for account-service; record for the recommendation-service task. Account Service only needs D1/D4 to be stable so the consumer can rely on them. |

### Notes

- No security change is needed: the existing owner rule covers the new path, and `GET`/`PUT /users/{userId}/preferences` are already listed under Customer (owner) in the spec. A Recommendation → Account call works only with the customer's forwarded token, which matches the spec.
- No Kafka, no new library and no new aggregate: `Preference` is a value object inside the `User` aggregate.
- Pre-existing, not A3-specific: other `String` fields (e.g. address fields) longer than 255 characters would also hit the database limit and return 500 instead of 400. Not fixed here; it can be handled together with D3 if wanted.

**Files created/modified:**

- `account-service/PROMPT_HISTORY.md` (this entry only)

---

## Prompt 11

**Date:** 2026-10-13

**Goal:** Record the team's decisions on the open A3 points (D1–D8 from Prompt 10) before implementation. No implementation or spec changes.

**Exact user prompt:**

````text
For D1: request { "preference": "..." }, response { "userId": "...", "preference": "..." }. D2: leave it as one free-text string following domain model. Don't create any structured preferences for now. D3: Since specs are not considering maximum length, for now leave it without validation. D4: 200 and "preference": null. D5: null, missing and blank if put then clear preference. D6: PreferenceUpdated only if value was changed; internal event, not Kafka. D7: preferences are not added to the usual UserResponse. D8: leave the Recommendation Service integration for later; it belongs to the next service implementation.
````

**Decisions for A3:**

| # | Decision |
|---|---|
| D1 | `PreferenceUpdateRequest` = `{ "preference": String }`; `PreferenceResponse` = `{ "userId", "preference" }`. |
| D2 | `Preference` stays one free-text String (domain-model.md 4.3). No structured preference fields. |
| D3 | No maximum-length validation (the specs define none). |
| D4 | `GET /users/{userId}/preferences` for an existing user with no saved preference → 200 with `"preference": null`. Unknown user → 404 `ResourceNotFound`. |
| D5 | `PUT` with `preference` null, missing or blank clears the saved preference. |
| D6 | `PreferenceUpdated` is raised only when the value actually changes; internal domain event, not Kafka. |
| D7 | The preference is not added to `UserResponse`. |
| D8 | Recommendation Service integration is deferred to the recommendation-service implementation. |

**Notes:**

- D3 consequence: without a length limit or validation, a value longer than the column length (JPA default `varchar(255)`) is expected to be rejected by the database and returned as 500 `InternalServiceError` by the current catch-all (not verified by a test).
- D5: "blank" is stored as null, so clearing an already empty preference raises no `PreferenceUpdated` (D6).
- `specs/api-endpoints.md` has no A3 DTO table; D1 is recorded here only. Adding it to the specs needs a separate request.

**Files created/modified:**

- `account-service/PROMPT_HISTORY.md` (this entry only)
