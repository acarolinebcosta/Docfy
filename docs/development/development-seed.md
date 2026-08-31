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

| Owner | Title | Category | Initial status |
|---|---|---|---|
| Ana | Quality Policy | Regulation | `DRAFT` |
| Ana | Information Security Policy | Regulation | `IN_REVIEW` |
| Ana | Software Release Checklist | Other | `APPROVED` |
| Ana | Operational Procedure | Other | `ARCHIVED` |
| João | Architecture Guidelines | Other | `DRAFT` |
| João | Incident Response Procedure | Notice | `IN_REVIEW` |
| João | Supplier Agreement | Contract | `APPROVED` |
| João | Meeting Minutes | Meeting Minutes | `ARCHIVED` |
| Ana | Ata de Revisão do MVP | Meeting Minutes | `APPROVED` |
| Ana | Certificado de Treinamento | Certificate | `DRAFT` |
| João | Comunicado de Manutenção | Notice | `IN_REVIEW` |
| João | Contrato de Prestação de Serviço | Contract | `DRAFT` |
| João | Ofício de Governança | Official Letter | `APPROVED` |

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

Three documents contain small, synthetic UTF-8 text attachments:

| Document | Attachment |
|---|---|
| Ata de Revisão do MVP | `ata-revisao-mvp.txt` |
| Certificado de Treinamento | `certificado-treinamento.txt` |
| Contrato de Prestação de Serviço | `contrato-prestacao-servico.txt` |

These resources contain no real personal, corporate or secret information.
They are uploaded through the production application service and file
validator, not inserted directly into storage or the database.

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
identified by creator and title, and attachments by document and original
filename. On restart, existing seed records are reused and their workflow is
not replayed, so users, documents, attachments and audit events are not
duplicated.

Missing canonical records can be created without requiring the database to be
globally empty. The initialization runs inside one transaction; a failure
rolls back newly created users, documents, workflow transitions, attachment
metadata and audit events together. Stored files created during a transaction
are removed by rollback compensation.

## Testing

Development Seed is not the same as automated test fixtures.

Automated tests remain responsible for creating and isolating their own test
data. The regular test profile does not activate the seed, and the seed does
not replace future API-based test data management for QA scenarios.
