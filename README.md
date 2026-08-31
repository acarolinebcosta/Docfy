# Docfy

Docfy is a full-stack document management platform and a public Quality
Engineering case study. The application combines a real Spring Boot API,
PostgreSQL persistence and a React frontend with security, testability and
traceability designed into the product.

## MVP capabilities

- JWT authentication and role-based authorization (`ADMIN`, `MANAGER`,
  `COLLABORATOR`);
- document creation, metadata editing and lifecycle workflow;
- immutable human-readable document codes;
- persisted category catalog;
- authorization-aware pagination, search and filters;
- local file upload, listing and authenticated download;
- immutable workflow audit trail and correlation IDs;
- opt-in development dataset;
- OpenAPI specification and Swagger UI.

The Spring Boot backend and PostgreSQL are the runtime source of truth. The
frontend does not provide a fake database or bypass backend authorization.

## Architecture

```text
React + TypeScript
        |
        | /api
        v
Spring Boot + Spring Security
        |
        +--> PostgreSQL (domain data and file metadata)
        |
        +--> configurable file storage (binary content)
```

See [System Architecture](docs/architecture/system-architecture.md) for the
detailed design.

## Local development

Requirements:

- Java 21;
- Node.js 24;
- Docker with Compose;
- npm.

Create the local environment file and start PostgreSQL:

```bash
cp .env.example .env
docker compose up -d
```

Start the backend with local configuration and the optional development seed:

```bash
cd backend
export SPRING_PROFILES_ACTIVE=local,dev
export JWT_SECRET='replace-with-a-local-secret-of-at-least-32-bytes'
export DOCFY_DEV_SEED_PASSWORD='replace-with-a-local-password'
./mvnw spring-boot:run
```

In another terminal, start the frontend:

```bash
cd frontend
npm ci
npm run dev
```

Real local secrets belong only in environment variables or the ignored `.env`
file. The values above are placeholders.

## File storage

Development binary storage defaults to `backend/data/files` when the backend
is started from its module directory. It can be changed with
`DOCFY_FILE_STORAGE_PATH`. Upload limits can be configured with
`DOCFY_FILE_MAX_SIZE` and `DOCFY_FILE_REQUEST_MAX_SIZE`.

See [File Storage](docs/development/file-storage.md) for supported formats,
validation and consistency behavior.

## API documentation

With the backend running:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

API operations under `/api/v1` remain protected according to their normal JWT
and document authorization rules.

## Validation

Backend:

```bash
cd backend
JWT_SECRET='docfy-test-secret-key-with-at-least-32-bytes' ./mvnw clean test
```

Frontend:

```bash
cd frontend
npm run lint
npm test
npm run build
```

## Documentation

- [Product Definition](docs/product/product-definition.md)
- [Quality Principles](docs/quality/quality-principles.md)
- [Initial Quality Risk Assessment](docs/quality/initial-risk-assessment.md)
- [Test Strategy](docs/quality/test-strategy.md)
- [System Architecture](docs/architecture/system-architecture.md)
- [Development Seed](docs/development/development-seed.md)

> Quality is a shared responsibility.
