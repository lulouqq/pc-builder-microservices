# account-service

In progress. Responsibilities: users, profiles, addresses, preferences (user stories A1–A3).

Port: 8084, base path `/api`.

## Implemented

| Story | Method | Endpoint | Response |
|---|---|---|---|
| A1 | POST | `/users` | 201 Created (`UserResponse`) |
| A1 | GET | `/users/{userId}` | 200 OK (`UserResponse`) |

- Passwords are hashed with BCrypt (`spring-security-crypto`); the plain password and the hash are never returned.
- `UserRegistered` is an internal domain event (published in-process by Spring Data on save), not a Kafka event.
- Errors: `ValidationFailed` (400) for missing required fields, a duplicate username or a malformed body; `ResourceNotFound` (404) for an unknown user.

Not yet implemented: profile update, addresses, authentication (A2) and preferences (A3).

## Running

```bash
./mvnw test -pl account-service
./mvnw spring-boot:run -pl account-service
```

H2 console: http://localhost:8084/api/h2-console (JDBC URL `jdbc:h2:mem:accountdb`, user `sa`, no password).
