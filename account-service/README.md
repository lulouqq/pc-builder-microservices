# account-service

In progress. Responsibilities: users, profiles, addresses, preferences (user stories A1–A3).

Port: 8084, base path `/api`.

## Implemented

| Story | Method | Endpoint | Response |
|---|---|---|---|
| A1 | POST | `/users` | 201 Created (`UserResponse`) |
| A1 | GET | `/users/{userId}` | 200 OK (`UserResponse`) |
| A1 | PUT | `/users/{userId}/profile` | 200 OK (`UserResponse`) |
| A1 | GET | `/users/{userId}/addresses` | 200 OK (array of `AddressResponse`) |
| A1 | POST | `/users/{userId}/addresses` | 201 Created (`AddressResponse`) |
| A1 | PUT | `/users/{userId}/addresses/{addressId}` | 200 OK (`AddressResponse`) |
| A1 | DELETE | `/users/{userId}/addresses/{addressId}` | 204 No Content |
| A2 | POST | `/auth/login` | 200 OK (`AuthenticationResponse`) |

- Passwords are hashed with BCrypt; the plain password and the hash are never returned.
- `PUT /users/{userId}/profile` replaces the whole profile (`firstName`, `lastName`, both optional; a missing field is cleared). Email, phone and password are not changed by it.
- Addresses belong to the `User` aggregate. All address fields are required (`streetNo` and `postCode` are integers); `PUT` replaces the whole address.
- `UserRegistered` and `UserProfileUpdated` are internal domain events (published in-process by Spring Data on save), not Kafka events. `UserProfileUpdated` is raised only when the profile actually changes.
- Errors: `ValidationFailed` (400) for missing required fields, a duplicate username or a malformed body; `AuthenticationRequired` (401) and `AccessDenied` (403), see below; `ResourceNotFound` (404) for an unknown user, or an address that does not exist or belongs to another user; `InternalServiceError` (500) for unexpected failures.

## Authentication (A2)

Design: `specs/api-endpoints.md` "Authentication and Authorisation" and `specs/architecture.md` decision 11. Spring Security runs as a stateless OAuth2 resource server; there are no sessions.

- `POST /auth/login` with `{"username", "password"}` returns `{"token", "tokenType": "Bearer", "expiresAt", "userId", "role"}`. The token is an HS256 JWT with the claims `sub` (user id), `role` (`CUSTOMER` / `ADMIN`), `iat` and `exp`. It is valid for 1 hour; there is no refresh or logout.
- Send the token as `Authorization: Bearer <token>`.
- Public: `POST /users`, `POST /auth/login` and the H2 console. Every `/users/{userId}/...` endpoint (and `GET /users/{userId}`) is only for the user in the token: another user's id returns 403 before the user is looked up (so an unknown id also returns 403). The `ADMIN` role does not bypass this check.
- `401 AuthenticationRequired`: wrong username or password (same message for both), or a missing, malformed, expired or wrongly signed token. `400 ValidationFailed`: missing username or password.
- `POST /users` always creates a `CUSTOMER`. One `ADMIN` account is created at startup from configuration if its username does not exist yet.

### Configuration

Secrets are read from environment variables and have no defaults, so the service does not start without them:

| Variable | Property | Required | Default |
|---|---|---|---|
| `JWT_SECRET` | `security.jwt.secret` | yes, at least 32 bytes; the same value in every service | — |
| `ADMIN_PASSWORD` | `security.admin.password` | yes | — |
| `ADMIN_USERNAME` | `security.admin.username` | no | `admin` |
| `ADMIN_EMAIL` | `security.admin.email` | no | `admin@pcbuilder.local` |
| `ADMIN_PHONE` | `security.admin.phone` | no | `0000000000` |

Tests use the `test` profile (`src/test/resources/application-test.yml`), which sets test-only values.

Not yet implemented: preferences (A3).

## Running

```bash
./mvnw test -pl account-service
JWT_SECRET='<at least 32 bytes>' ADMIN_PASSWORD='<admin password>' ./mvnw spring-boot:run -pl account-service
```

H2 console: http://localhost:8084/api/h2-console (JDBC URL `jdbc:h2:mem:accountdb`, user `sa`, no password).
