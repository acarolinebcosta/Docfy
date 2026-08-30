# Docfy — System Architecture

**Version:** 0.1  
**Status:** Draft  
**Scope:** MVP

**Related documents:**

- [Product Definition](../product/product-definition.md)
- [Quality Principles](../quality/quality-principles.md)
- [Initial Quality Risk Assessment](../quality/initial-risk-assessment.md)
- [Test Strategy](../quality/test-strategy.md)

---

## 1. Purpose

This document defines the initial technical architecture for Docfy.

The architecture is designed to support:

- product requirements;
- business rules;
- security and authorization;
- maintainability;
- testability;
- observability;
- reliability;
- automated quality validation;
- continuous delivery.

The objective is to use a simple architecture that is appropriate for the current product scope while allowing the application to evolve without unnecessary complexity.

---

## 2. Architecture Principles

The initial Docfy architecture follows these principles:

- keep the architecture simple while the product is small;
- separate frontend, backend and persistence responsibilities;
- enforce business rules in the backend;
- enforce authorization on the server side;
- design APIs with predictable contracts;
- preserve data consistency through transactions;
- treat testability as an architectural concern;
- make failures observable;
- externalize configuration;
- avoid unnecessary infrastructure;
- evolve architecture according to real product needs.

---

## 3. Architecture Style

Docfy will initially use a modular monolithic backend with a separate web frontend.

High-level architecture:

```text
┌─────────────────────────────────────────────┐
│                  User                       │
└─────────────────────┬───────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────┐
│              Web Application                │
│                                             │
│           React + TypeScript                │
│                                             │
│  Pages                                      │
│  Components                                 │
│  Forms                                      │
│  Accessibility Semantics                    │
│  Client-side State                          │
└─────────────────────┬───────────────────────┘
                      │
                      │ HTTPS / REST / JSON
                      ▼
┌─────────────────────────────────────────────┐
│               Backend API                   │
│                                             │
│          Java + Spring Boot                 │
│                                             │
│  Authentication                             │
│  Authorization                              │
│  Document Management                        │
│  Workflow                                   │
│  Search                                     │
│  Audit                                      │
│  File Management                            │
└───────────────┬─────────────────┬───────────┘
                │                 │
                ▼                 ▼
      ┌──────────────────┐   ┌─────────────────┐
      │    PostgreSQL    │   │  File Storage   │
      │                  │   │                 │
      │ Users            │   │ Document files  │
      │ Documents        │   │                 │
      │ Categories       │   │                 │
      │ Audit Events     │   │                 │
      └──────────────────┘   └─────────────────┘
```

---

## 4. Why a Modular Monolith

A modular monolith was selected instead of microservices for the MVP.

The current Docfy domain does not require independently deployable distributed services.

Using microservices at this stage would introduce additional complexity such as:

- distributed communication;
- service discovery;
- distributed tracing;
- network failure handling;
- multiple deployments;
- contract synchronization;
- additional infrastructure.

Without a demonstrated product need, this would represent unnecessary complexity.

The modular monolith provides:

- simpler development;
- simpler deployment;
- easier debugging;
- easier integration testing;
- transactional consistency;
- lower infrastructure cost;
- clear module boundaries.

Modules should still remain logically separated so that future architectural evolution remains possible.

---

## 5. Technology Stack

| Area | Technology |
|---|---|
| Frontend | React + TypeScript |
| Backend | Java + Spring Boot |
| Database | PostgreSQL |
| API | REST + JSON |
| API Contract | OpenAPI / Swagger |
| Authentication | JWT |
| Local Infrastructure | Docker Compose |
| Backend Unit Tests | JUnit |
| API Tests | Python + Pytest + Requests |
| Web E2E Tests | Playwright + TypeScript |
| Accessibility Automation | Playwright + axe-core |
| Performance Tests | k6 |
| CI/CD | GitHub Actions |
| Observability | Structured Logs + Correlation IDs |

The stack may evolve as implementation constraints and product requirements become clearer.

---

## 6. Frontend Architecture

The frontend will be implemented using React and TypeScript.

Primary responsibilities include:

- presentation;
- user interaction;
- form validation feedback;
- workflow visualization;
- accessibility semantics;
- navigation;
- API communication.

The frontend must not be responsible for enforcing critical business rules.

For example:

```text
COLLABORATOR
     ↓
UI hides "Approve" action
```

This improves the user experience, but it does not provide actual authorization.

The backend must still reject unauthorized approval attempts.

---

## 7. Frontend Testability

Critical UI elements should expose stable and meaningful selectors.

Preferred approaches include:

- accessible roles;
- accessible names;
- semantic HTML;
- labels;
- stable test identifiers where semantics are insufficient.

Example:

```html
<button aria-label="Approve document">
  Approve
</button>
```

Where a stable identifier is required:

```html
<button data-testid="approve-document">
  Approve
</button>
```

Test automation should prefer user-visible or semantic locators when they provide sufficient stability.

Implementation should avoid fragile selectors based on:

- DOM hierarchy;
- CSS position;
- dynamic class names;
- generated identifiers.

---

## 8. Accessibility by Design

Accessibility must be considered during frontend implementation rather than added after feature completion.

The interface should prioritize:

- semantic HTML;
- correct heading hierarchy;
- labelled form controls;
- keyboard navigation;
- logical focus order;
- visible focus;
- accessible names;
- meaningful error messages;
- screen reader compatibility;
- adequate contrast.

Critical journeys should target WCAG 2.2 Level AA.

---

## 9. Backend Architecture

The backend will use Java and Spring Boot.

Initial logical modules include:

```text
backend
│
├── authentication
├── users
├── documents
├── categories
├── workflow
├── authorization
├── audit
├── files
└── shared
```

Each module should encapsulate its related behavior and business rules.

---

## 10. Layered Backend Structure

Within modules, the backend may follow a structure similar to:

```text
Controller
    ↓
Application / Service
    ↓
Domain
    ↓
Repository
    ↓
Database
```

### Controller

Responsible for:

- HTTP requests;
- input parsing;
- response formatting;
- HTTP status codes.

### Application / Service

Responsible for:

- use-case orchestration;
- workflow coordination;
- transactions.

### Domain

Responsible for:

- business rules;
- domain state;
- lifecycle constraints.

### Repository

Responsible for:

- persistence abstraction;
- database communication.

Business logic should not be concentrated in controllers.

---

## 11. REST API

The frontend and backend will communicate through REST over HTTPS using JSON.

Example:

```text
POST  /api/v1/documents
GET   /api/v1/documents
GET   /api/v1/documents/{id}
PATCH /api/v1/documents/{id}
POST  /api/v1/documents/{id}/submit
POST  /api/v1/documents/{id}/approve
POST  /api/v1/documents/{id}/reject
POST  /api/v1/documents/{id}/archive
GET   /api/v1/documents/{id}/audit
```

The final API structure may evolve during implementation.

---

## 12. API Versioning

Initial endpoints should use explicit versioning:

```text
/api/v1/
```

This provides room for future contract evolution without silently breaking clients.

---

## 13. OpenAPI Contract

The backend should expose an OpenAPI specification.

The contract should document:

- endpoints;
- request schemas;
- response schemas;
- authentication requirements;
- status codes;
- validation errors.

This supports:

- API documentation;
- development integration;
- automated contract validation;
- test design;
- detection of breaking changes.

---

## 14. Structured Error Responses

API errors should follow a predictable structure.

Example:

```json
{
  "timestamp": "2026-08-22T18:00:00Z",
  "status": 403,
  "error": "FORBIDDEN",
  "message": "User is not allowed to approve this document",
  "path": "/api/v1/documents/123/approve",
  "correlationId": "a82f19"
}
```

Structured errors improve:

- API usability;
- frontend behavior;
- automated assertions;
- debugging;
- observability.

---

## 15. Authentication

Authentication will initially use JWT.

Expected flow:

```text
User
  ↓
Login
  ↓
Credentials validated
  ↓
JWT issued
  ↓
Client sends token
  ↓
Backend validates token
  ↓
Authorized request
```

Tokens should contain only information necessary for authentication and authorization.

Sensitive information must not be stored in JWT payloads.

---

## 16. Authorization

Authorization must always be enforced by the backend.

Initial roles:

```text
ADMIN
MANAGER
COLLABORATOR
```

Example permission model:

| Operation | Collaborator | Manager | Admin |
|---|---:|---:|---:|
| Create document | Yes | Yes | Yes |
| View own document, any status | Yes | Yes | Yes |
| View another user's draft | No | Yes | Yes |
| View another user's document in review | No | Yes | Yes |
| View another user's approved document | Yes | Yes | Yes |
| View another user's archived document | No | Yes | Yes |
| Edit own draft | Yes | Yes | Yes |
| Edit another user's draft | No | Yes | Yes |
| Edit non-draft document | No | No | No |
| Submit for review | Yes | Yes | Yes |
| Review document | No | Yes | Yes |
| Approve document | No | Yes | Yes |
| Reject document | No | Yes | Yes |
| Archive approved document | No | Yes | Yes |
| Manage users | No | No | Yes |
| View audit information | No | Yes | Yes |

The final permission model may evolve.

### Document visibility policy

Document visibility is evaluated using the authenticated user's role, the document creator and the document status:

- `ADMIN` and `MANAGER` have unrestricted read visibility;
- `COLLABORATOR` can read their own documents in every status;
- `COLLABORATOR` can read documents created by other users only in `APPROVED` status.

This policy is implemented as one reusable application authorization component. Direct lookup evaluates the fetched document against the policy. Listing and future search apply the equivalent predicate in the database query before pagination:

```text
ADMIN or MANAGER
    → all documents

COLLABORATOR
    → created_by = authenticated user
      OR status = APPROVED
```

Filtering an already paginated, unrestricted result in memory is not permitted because it would produce incorrect totals, unstable pages and possible metadata disclosure.

If direct lookup finds a document that is not visible to the authenticated user, the application raises the same not-found outcome used for an unknown identifier.

The HTTP response is `404 Not Found`, with the existing safe `Document not found` contract, so callers cannot distinguish a missing document from a concealed document.

### Document listing contract

`GET /api/v1/documents` returns an explicit paginated response:

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

Page numbering is zero-based.

The default page is `0`, the default size is `20`, and the maximum size is `100`.

Negative page numbers, non-positive sizes and sizes above the maximum are rejected with `400 Bad Request`; invalid sizes are not silently clamped.

Results are ordered by `updatedAt DESC` and then `id ASC`.

The identifier is the deterministic tie-breaker required for stable navigation between pages.

### Document edit policy

Document editing is evaluated independently from document visibility.

Metadata updates are allowed only while the document is in `DRAFT` status.

The initial edit rules are:

| Role | Own `DRAFT` | Other user's `DRAFT` | `IN_REVIEW` | `APPROVED` | `ARCHIVED` |
|---|---:|---:|---:|---:|---:|
| `ADMIN` | Yes | Yes | No | No | No |
| `MANAGER` | Yes | Yes | No | No | No |
| `COLLABORATOR` | Yes | No | No | No | No |

The visibility policy is evaluated before the edit policy.

If the authenticated user cannot view the document, the API returns `404 Not Found`, using the same safe response as an unknown document identifier.

If the authenticated user can view the document but is not allowed to edit it, the API returns `403 Forbidden`.

This distinction prevents document enumeration while preserving an explicit authorization response for resources already visible to the caller.

`PATCH /api/v1/documents/{id}` currently allows partial updates to:

- `title`;
- `description`.

The following fields cannot be modified through this operation:

- document identifier;
- status;
- creator;
- creation timestamp;
- update timestamp.

For PATCH semantics:

- omitted fields remain unchanged;
- `description: null` explicitly clears the description;
- `title: null` is invalid;
- blank titles are invalid;
- titles longer than 255 characters are invalid;
- an empty PATCH document is invalid.

---

## 17. Authorization and Product Risk

This architectural decision directly addresses:

```text
BR-003
Only MANAGER and ADMIN can approve/reject
      ↓
RISK-002
Unauthorized approval/rejection
      ↓
Backend authorization enforcement
```

And:

```text
BR-010
Users must not access unauthorized documents
      ↓
RISK-001
Unauthorized document access
      ↓
Backend authorization enforced for every resource request
```

Document editing also addresses:

```text
BR-004
Non-draft documents cannot be directly modified
      ↓
RISK-004
Approved or protected document modification
      ↓
Visibility validation
      ↓
Edit authorization
      ↓
Lifecycle-state enforcement
```

Frontend visibility rules must never replace backend authorization.

---

## 18. Document Lifecycle

Initial document lifecycle:

```text
DRAFT
  ↓
IN_REVIEW
  ├── approve → APPROVED → ARCHIVED
  └── reject  → DRAFT
```

Rejection returns the document directly to `DRAFT`; it does not create a separate persisted state.

The backend domain layer must enforce valid transitions.

### Workflow API

Document lifecycle transitions are exposed through explicit action endpoints:

```text
POST /api/v1/documents/{id}/submit
POST /api/v1/documents/{id}/approve
POST /api/v1/documents/{id}/reject
POST /api/v1/documents/{id}/archive
```

Successful operations return the updated document representation.

The initial action authorization model is:

| Action                        | Collaborator | Manager | Admin |
| ----------------------------- | -----------: | ------: | ----: |
| Submit own `DRAFT`            |          Yes |     Yes |   Yes |
| Submit another user's `DRAFT` |           No |     Yes |   Yes |
| Approve `IN_REVIEW`           |           No |     Yes |   Yes |
| Reject `IN_REVIEW`            |           No |     Yes |   Yes |
| Archive `APPROVED`            |           No |     Yes |   Yes |

Workflow authorization is separated from lifecycle-state validation.

The application evaluates a workflow request in the following order:

```text
Load document
    ↓
Document visible to actor?
    ├── No → 404 Not Found
    ↓
Actor authorized for action?
    ├── No → 403 Forbidden
    ↓
Current state allows transition?
    ├── No → 409 Conflict
    ↓
Apply lifecycle transition
```

The visibility check occurs before action authorization so concealed resources cannot be enumerated.

A missing document and a document that exists but is not visible to the caller therefore produce the same safe `404 Not Found` contract.

A caller who can view the document but does not have permission to execute the requested workflow action receives `403 Forbidden`.

An authorized caller attempting a transition that conflicts with the current document state receives `409 Conflict`.


---

## 19. State Transition Enforcement

Lifecycle invariants are enforced by the `Document` domain entity rather than by directly assigning document status from controllers or application services.

The domain exposes explicit transition operations:

```text
submitForReview()
approve()
reject()
archive()
```

No public generic status setter is exposed.

Valid transitions are:

```text
DRAFT → IN_REVIEW
IN_REVIEW → APPROVED
IN_REVIEW → DRAFT
APPROVED → ARCHIVED
```

Every other transition is invalid.

Examples:

```text
DRAFT → APPROVED
```

must fail.

```text
ARCHIVED → IN_REVIEW
```

must fail.

```text
APPROVED → DRAFT
```

must fail.

Invalid lifecycle transitions raise a domain exception that is translated by the HTTP layer to `409 Conflict`.

Workflow responsibilities are separated as follows:

```text
DocumentVisibilityPolicy
    → determines whether the resource may be disclosed

DocumentWorkflowPolicy
    → determines whether the actor may execute the requested action

DocumentWorkflowService
    → coordinates resource lookup, visibility and authorization

Document
    → enforces valid lifecycle transitions
```

Workflow mutations execute inside transactions. Documents loaded through the repository remain managed entities, allowing lifecycle changes to be persisted through JPA dirty checking without an explicit repository `save()` call.

This addresses:

```text
RISK-003 — Invalid document state transition
```

---

## 20. Transactional Workflow

Document lifecycle transitions and audit persistence execute inside the same application transaction.

The implemented flow is:

```text
Workflow request
       ↓
Load document
       ↓
Validate visibility
       ↓
Validate action authorization
       ↓
Capture previous status
       ↓
Apply domain lifecycle transition
       ↓
Create audit event
       ↓
Persist audit event
       ↓
Commit transaction
```

If audit persistence fails, the workflow transaction rolls back both the document status change and the audit write. The system must not commit a lifecycle transition without its corresponding event. This directly addresses `RISK-012 — Partial update during failure`.

---

## 21. Audit Architecture

Every successful document lifecycle transition creates an immutable audit event in the same transaction as the status change.

The `document_audit_events` table stores:

- document identifier;
- actor identifier;
- action;
- previous status;
- new status;
- occurrence timestamp;
- correlation ID when available.

The implemented audit actions are:

```text
DOCUMENT_SUBMITTED
DOCUMENT_APPROVED
DOCUMENT_REJECTED
DOCUMENT_ARCHIVED
```

The workflow application service captures the previous status, applies the domain transition and delegates event persistence to the audit service. Audit events are append-only through product operations; no update or delete endpoint is exposed.

Audit history is available through:

```text
GET /api/v1/documents/{id}/audit
```

Events are returned in ascending occurrence order, with the event identifier as a deterministic tie-breaker. `ADMIN` and `MANAGER` can read audit history. `COLLABORATOR` receives `403 Forbidden` for a visible document. A missing or concealed document produces the same `404 Not Found` response used by document lookup.

Example:

```json
{
  "id": "c15a5154-cc3e-4592-b529-ab2910bd5c06",
  "documentId": "898636bf-90ff-4a4f-b1e9-66115ef6dba2",
  "actorId": "4d6935fb-22f9-4e62-bf53-b259cacb1951",
  "action": "DOCUMENT_APPROVED",
  "previousStatus": "IN_REVIEW",
  "newStatus": "APPROVED",
  "occurredAt": "2026-08-22T18:00:00Z",
  "correlationId": "a82f19dc-d9e0-4698-bc73-dd50b7445a76"
}
```

The correlation ID connects the lifecycle request, structured logs and the persisted evidence without exposing credentials or other sensitive user data.

---

## 22. Database

PostgreSQL will be used as the primary relational database.

The domain contains strongly related entities and requires transactional consistency.

Initial entities may include:

```text
User
Role
Document
Category
AuditEvent
FileMetadata
```

Potential relationships:

```text
User
  │
  └── Documents
          │
          ├── Category
          ├── Status
          ├── File Metadata
          └── Audit Events
```

---

## 23. Database Integrity

The database should reinforce critical invariants when appropriate.

Examples include:

- unique document identifiers;
- non-null mandatory fields;
- foreign-key relationships;
- valid relational references;
- controlled deletion behavior.

Business rules should not depend exclusively on database constraints, but persistence should provide an additional integrity layer.

---

## 24. Document Identifier

Each document must have a unique identifier.

Example:

```text
DOC-000001
DOC-000002
DOC-000003
```

The exact strategy will be defined during implementation.

Uniqueness must be enforced at the database level.

This reduces:

```text
RISK-006 — Duplicate document identifiers
```

---

## 25. File Storage

Document metadata and binary document files should remain logically separated.

PostgreSQL should store file metadata such as:

- original filename;
- content type;
- size;
- storage reference;
- upload timestamp;
- responsible user.

Binary storage may initially use local or container-backed storage for development.

The storage abstraction should allow future migration to external object storage without changing the core document domain.

---

## 26. File Validation

File validation must occur on the backend.

Validation should include:

- allowed formats;
- declared content type;
- actual content type where feasible;
- maximum file size;
- empty files;
- malformed uploads.

Client-side validation may improve UX but must not replace server-side validation.

This decision addresses:

```text
RISK-007 — Invalid or unsafe file upload
```

---

## 27. Listing and Search

Document listing and search must always apply the document visibility policy before returning or paginating results.

The architecture must avoid:

```text
Search all documents
       ↓
Return results
       ↓
Frontend hides unauthorized records
```

Instead:

```text
Authenticated user
       ↓
Authorization context
       ↓
Query only documents visible to the authenticated user
       ↓
Return filtered result set
```

This addresses:

```text
RISK-009 — Search exposes protected documents
```

---

## 28. Observability

Observability is considered part of product quality and testability.

Initial observability capabilities should include:

- structured logs;
- correlation IDs;
- application health information;
- meaningful error context.

Observability should support:

- development;
- automated test investigation;
- production troubleshooting;
- failure analysis.

---

## 29. Structured Logging

Backend logs should use a structured format.

Relevant log information may include:

- timestamp;
- log level;
- correlation ID;
- operation;
- user identifier when appropriate;
- document identifier;
- outcome;
- error type.

Sensitive information must not be logged.

Examples of information that should not appear in logs:

- passwords;
- authentication secrets;
- JWT values;
- document contents;
- sensitive personal information.

---

## 30. Correlation IDs

Each incoming request should have a correlation identifier.

Example:

```text
X-Correlation-ID: a82f19
```

Flow:

```text
Frontend Request
      ↓
Correlation ID
      ↓
Backend
      ↓
Service
      ↓
Database / Audit
      ↓
Logs
```

When an automated test fails, the correlation ID can be used to connect test evidence with backend logs.

Example:

```text
Playwright failure
      ↓
correlationId = a82f19
      ↓
Backend logs
      ↓
Root-cause investigation
```

---

## 31. Health Endpoint

The backend should provide a health endpoint.

Example:

```text
GET /actuator/health
```

Potential response:

```json
{
  "status": "UP"
}
```

This can be used by:

- local environment validation;
- Docker;
- CI pipelines;
- automated test setup;
- monitoring.

---

## 32. Testability Architecture

Testability requirements should influence implementation decisions.

Initial testability features include:

```text
Stable UI identifiers
Accessible semantics
Predictable API contracts
Structured errors
Synthetic test data
Resettable environments
Health endpoint
Structured logs
Correlation IDs
Controlled failure simulation
```

The goal is to avoid compensating for poor testability through fragile automation.

---

## 33. Test Data Support

The architecture should allow tests to create controlled data without relying entirely on the UI.

Potential mechanisms include:

- test-data APIs;
- fixtures;
- builders;
- database seed scripts;
- factories.

Example:

```text
Test needs:

MANAGER
+
IN_REVIEW document
        ↓
Test Data Factory
        ↓
Ready test state
```

instead of:

```text
Login through UI
Create document
Edit document
Submit document
Logout
Login as manager
Then start actual test
```

This reduces execution time and test coupling.

---

## 34. Environment Reset

Automated environments should support predictable state restoration.

Potential strategies include:

- database cleanup;
- transactional tests;
- isolated datasets;
- Docker container reset;
- migration + seed execution.

Test environments should remain reproducible.

---

## 35. Controlled Failure Simulation

Where technically feasible, the architecture should support controlled simulation of failures.

Examples:

```text
database timeout
storage unavailable
audit persistence failure
slow dependency
```

This enables reliability testing for scenarios that are difficult to reproduce naturally.

The mechanism should only be enabled in authorized test environments.

---

## 36. Local Development Environment

Docker Compose will be used to provide reproducible local infrastructure.

Initial composition may include:

```text
docker-compose
│
├── frontend
├── backend
└── postgres
```

Development may initially run frontend/backend outside containers while using PostgreSQL through Docker.

The final approach may evolve based on developer experience.

---

## 37. Configuration Management

Environment-specific configuration should remain externalized.

Examples:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
JWT_SECRET
FILE_STORAGE_PATH
```

Sensitive values must never be committed to Git.

Example files may be provided:

```text
.env.example
```

but real secrets must remain outside version control.

---

## 38. CI/CD Architecture

GitHub Actions will progressively validate Pull Requests.

Target workflow:

```text
Pull Request
      ↓
Static Analysis
      ↓
Build
      ↓
Unit Tests
      ↓
API Tests
      ↓
Smoke E2E
      ↓
Accessibility Checks
      ↓
Quality Gate
      ↓
Merge
```

Longer suites may execute separately.

Examples:

```text
Core regression → scheduled / release
Performance → scheduled / manual
Stress → manual
Soak → scheduled / pre-release
```

---

## 39. Quality Gate Architecture

Merge criteria should become executable whenever possible.

Future gate examples include:

```text
Build                     PASS
Unit Tests                PASS
API Critical Tests        PASS
Smoke Tests               PASS
Accessibility Critical    PASS
```

A failing required quality check should prevent integration into `main`.

---

## 40. Testing Architecture

Different test types should target the most appropriate architectural layer.

```text
┌──────────────────────────────────────────────┐
│ React                                        │
│                                              │
│ E2E                                          │
│ Accessibility                                │
│ Usability                                    │
└──────────────────────────────────────────────┘

┌──────────────────────────────────────────────┐
│ REST API                                     │
│                                              │
│ Functional API Tests                         │
│ Authorization                                │
│ Negative Tests                               │
│ Contract Tests                               │
└──────────────────────────────────────────────┘

┌──────────────────────────────────────────────┐
│ Spring Boot                                  │
│                                              │
│ Unit Tests                                   │
│ Integration Tests                            │
│ Domain Rules                                 │
└──────────────────────────────────────────────┘

┌──────────────────────────────────────────────┐
│ PostgreSQL                                   │
│                                              │
│ Integrity Validation                         │
│ Persistence                                  │
│ Audit Validation                             │
└──────────────────────────────────────────────┘

┌──────────────────────────────────────────────┐
│ Complete System                              │
│                                              │
│ Performance                                  │
│ Reliability                                  │
│ Compatibility                                │
│ Observability                                │
└──────────────────────────────────────────────┘
```

---

## 41. Security Considerations

Initial architecture should address basic security concerns including:

- secure password storage;
- JWT validation;
- backend authorization;
- least privilege;
- protected resources;
- safe file handling;
- secrets management;
- secure configuration;
- prevention of sensitive logging.

Specialized penetration testing is outside the initial QA scope but may be introduced later if required.

---

## 42. Reliability Considerations

Critical state-changing operations should favor consistency over partial success.

The architecture should support:

- transactions;
- idempotency where appropriate;
- meaningful failure responses;
- rollback;
- traceable operations;
- controlled retries where safe.

Retries must not create duplicated state-changing actions.

---

## 43. Concurrent Operations

The system should consider concurrent actions on the same document.

Example:

```text
Manager A
APPROVE
    \
     \
      Document IN_REVIEW
     /
    /
Manager B
REJECT
```

The backend must prevent ambiguous final states.

Potential techniques may include:

- optimistic locking;
- transactional validation;
- version fields.

The final implementation decision will be documented when the persistence model is defined.

This addresses:

```text
RISK-010 — Concurrent workflow conflict
```

---

## 44. Performance Considerations

Architecture should allow performance to be observed and tested.

Areas likely to require measurement include:

- authentication;
- document listing;
- search;
- filters;
- document creation;
- upload;
- concurrent access.

Database indexes and query design should be based on measured behavior rather than premature optimization.

---

## 45. Architectural Traceability

Architecture decisions should remain connected to product risks.

Example:

```text
BR-010
Users must not access unauthorized documents
        ↓
RISK-001
Unauthorized document access
        ↓
Architecture
Backend authorization
        ↓
Testing
API authorization tests
        ↓
Evidence
CI execution
```

Another example:

```text
BR-009
Auditability
        ↓
RISK-005
Incorrect audit history
        ↓
Architecture
Transactional audit
        ↓
Testing
API + Database validation
        ↓
Evidence
CI execution
```

---

## 46. Risk-to-Architecture Mapping

| Risk | Architectural Response |
|---|---|
| RISK-001 Unauthorized document access | Backend authorization |
| RISK-002 Unauthorized approval/rejection | Role-based backend authorization |
| RISK-003 Invalid state transition | Domain-level lifecycle validation |
| RISK-004 Approved document modification | Backend state and edit-policy validation |
| RISK-005 Incorrect audit history | Transactional audit persistence |
| RISK-006 Duplicate identifiers | Database uniqueness constraint |
| RISK-007 Invalid file upload | Backend file validation |
| RISK-008 Authentication/session failure | Centralized authentication validation |
| RISK-009 Search exposes protected documents | Authorization-aware queries |
| RISK-010 Concurrent workflow conflict | Transaction/locking strategy |
| RISK-011 Missing required information | API/domain validation |
| RISK-012 Partial update during failure | Transaction boundaries |
| RISK-013 Search performance | Measurable queries and indexing |
| RISK-014 Inaccessible workflows | Accessibility by design |
| RISK-015 Poor workflow usability | Consistent UI patterns and evaluation |

---

## 47. Architectural Decision Records

Important architectural decisions should eventually be documented as Architecture Decision Records (ADRs).

Potential ADRs include:

```text
ADR-001 — Modular monolith
ADR-002 — PostgreSQL as primary persistence
ADR-003 — JWT authentication
ADR-004 — Backend-enforced authorization
ADR-005 — REST + OpenAPI
ADR-006 — Transactional workflow and audit
ADR-007 — Correlation IDs
```

ADRs should explain:

- context;
- decision;
- alternatives considered;
- consequences.

They will be introduced when the decisions begin affecting implementation.

---

## 48. Architecture Evolution

The current architecture is intentionally simple.

Potential future evolution may include:

- external object storage;
- centralized monitoring;
- caching;
- asynchronous processing;
- notifications;
- advanced search;
- document versioning;
- multi-tenancy;
- AI-assisted document classification.

These capabilities should only be introduced when justified by product requirements.

---

## 49. What Is Intentionally Not Included

The MVP architecture will not initially include:

- microservices;
- Kubernetes;
- event-driven distributed architecture;
- service mesh;
- distributed databases;
- complex messaging infrastructure;
- multi-region infrastructure.

Adding technology without a concrete product or engineering need would increase complexity without improving the current product.

---

## 50. Architecture Review Criteria

Architecture changes should be evaluated against:

- product value;
- product risks;
- maintainability;
- security;
- testability;
- observability;
- reliability;
- performance;
- implementation complexity;
- operational complexity.

A more complex architecture should only be adopted when the benefits justify its costs.

---

## 51. Final Architecture Direction

The initial Docfy architecture can be summarized as:

```text
React + TypeScript
        ↓
REST + OpenAPI
        ↓
Java + Spring Boot
        ↓
PostgreSQL
        +
File Storage
```

Supported by:

```text
JWT Authentication
Backend Authorization
Transactions
Structured Errors
Structured Logs
Correlation IDs
Health Checks
Docker Compose
GitHub Actions
Testability by Design
```

And validated through:

```text
Unit
API
Integration
E2E
Database
Exploratory
Accessibility
Usability
Performance
Reliability
```

The architecture is designed not only to make Docfy functional, but also to make product quality measurable, testable and observable throughout its lifecycle.
