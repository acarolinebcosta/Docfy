# Docfy Development Seed

## Purpose

The Development Seed provides a small, predictable dataset for local
development, frontend integration, demonstrations, exploratory testing and
manual authorization checks.

It is development infrastructure, not a business feature and not an automated
test fixture.

## Safety

**Development only. Never enable the `dev` profile in production.**

The seed is loaded only when the Spring `dev` profile is explicitly active. It
is not loaded by the default profile or by the automated test suite. Seed data
is created through the application's domain and application services; no demo
records are stored in Flyway migrations.

## Users

| Email | Role |
|---|---|
| `admin@docfy.local` | `ADMIN` |
| `manager@docfy.local` | `MANAGER` |
| `ana@docfy.local` | `COLLABORATOR` |
| `joao@docfy.local` | `COLLABORATOR` |

All four users receive the password supplied through
`DOCFY_DEV_SEED_PASSWORD`. The application encodes it with the production
`PasswordEncoder` before persistence. Neither the password nor its hash is
logged.

Use a stable local value while reusing an existing development database. If an
existing seed identity has a different role, activation state or password, the
application fails safely instead of silently overwriting that identity.

## Documents

| Owner | Title | Initial status |
|---|---|---|
| Ana | Quality Policy | `DRAFT` |
| Ana | Information Security Policy | `IN_REVIEW` |
| Ana | Software Release Checklist | `APPROVED` |
| Ana | Operational Procedure | `ARCHIVED` |
| João | Architecture Guidelines | `DRAFT` |
| João | Incident Response Procedure | `IN_REVIEW` |
| João | Supplier Agreement | `APPROVED` |
| João | Meeting Minutes | `ARCHIVED` |

Advanced states are reached through the normal workflow services. The
Information Security Policy demonstrates rejection and resubmission:

```text
DRAFT
  -> DOCUMENT_SUBMITTED -> IN_REVIEW
  -> DOCUMENT_REJECTED  -> DRAFT
  -> DOCUMENT_SUBMITTED -> IN_REVIEW
```

Approved and archived documents likewise contain their submitted, approved
and archived audit events. Startup events have no HTTP request correlation ID,
so their `correlationId` is `null`.

## Running

From a fresh clone, create the local Compose environment file and start
PostgreSQL:

```bash
cp .env.example .env
docker compose up -d
```

Then start the backend with both local database configuration and the opt-in
development seed profile:

```bash
cd backend
export SPRING_PROFILES_ACTIVE=local,dev
export JWT_SECRET='replace-with-a-local-secret-of-at-least-32-bytes'
export DOCFY_DEV_SEED_PASSWORD='replace-with-a-local-password'
./mvnw spring-boot:run
```

The values above are placeholders. Choose local values and never commit the
real `.env` file.

## Idempotency

Users are identified by their deterministic email addresses. Documents are
identified by creator and title. On restart, existing seed records are reused
and their workflow is not replayed, so users, documents and audit events are
not duplicated.

Missing canonical records can be created without requiring the database to be
globally empty. The initialization runs inside one transaction; a failure
rolls back newly created users, documents, workflow transitions and audit
events together.

## Testing

Development Seed is not the same as automated test fixtures.

Automated tests remain responsible for creating and isolating their own test
data. The regular test profile does not activate the seed, and the seed does
not replace future API-based test data management for QA scenarios.
