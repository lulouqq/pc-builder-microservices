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

- Passwords are hashed with BCrypt (`spring-security-crypto`); the plain password and the hash are never returned.
- `PUT /users/{userId}/profile` replaces the whole profile (`firstName`, `lastName`, both optional; a missing field is cleared). Email, phone and password are not changed by it.
- Addresses belong to the `User` aggregate. All address fields are required (`streetNo` and `postCode` are integers); `PUT` replaces the whole address.
- `UserRegistered` and `UserProfileUpdated` are internal domain events (published in-process by Spring Data on save), not Kafka events. `UserProfileUpdated` is raised only when the profile actually changes.
- Errors: `ValidationFailed` (400) for missing required fields, a duplicate username or a malformed body; `ResourceNotFound` (404) for an unknown user, or an address that does not exist or belongs to another user; `InternalServiceError` (500) for unexpected failures.

Not yet implemented: authentication (A2) and preferences (A3).

## Running

```bash
./mvnw test -pl account-service
./mvnw spring-boot:run -pl account-service
```

H2 console: http://localhost:8084/api/h2-console (JDBC URL `jdbc:h2:mem:accountdb`, user `sa`, no password).
