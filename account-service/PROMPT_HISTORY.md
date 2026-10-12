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
