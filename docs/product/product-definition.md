# Docfy — Product Definition

**Version:** 0.1
**Status:** Draft
**Product:** Docfy

---

## 1. Product Vision

Docfy is a web-based document management platform designed to help teams organize, manage, review, approve and track documents throughout their lifecycle.

The product aims to provide a simple, accessible and reliable experience while maintaining traceability, permissions and document history.

Docfy also serves as a Quality Engineering case study, where product quality is considered from product discovery through development, testing, delivery and continuous improvement.

---

## 2. Problem

Organizations frequently manage documents through disconnected tools, shared folders, email attachments and manual processes.

This can lead to:

* difficulty finding documents;
* unclear document ownership;
* lack of version control;
* missing approval history;
* unauthorized changes;
* outdated documents remaining in use;
* poor visibility of document status;
* inconsistent workflows.

Docfy aims to centralize these activities in a structured and traceable workflow.

---

## 3. Value Proposition

Docfy provides a centralized environment where users can:

* create and organize documents;
* upload files;
* classify documents;
* submit documents for review;
* approve or reject documents;
* control document lifecycle;
* search and filter documents;
* manage access according to user roles;
* track document history and changes.

---

## 4. Target Users

Docfy is initially designed for small and medium-sized organizations that need a simple document management workflow.

Potential users include:

* administrative teams;
* educational institutions;
* small businesses;
* consulting companies;
* offices;
* internal operational teams.

---

## 5. Personas

### Administrator

Responsible for configuring and maintaining the platform.

Main capabilities:

* manage users;
* manage document categories;
* configure permissions;
* access all documents;
* access audit information.

### Manager

Responsible for document review and approval.

Main capabilities:

* create documents;
* review submitted documents;
* approve or reject documents;
* archive documents;
* access all documents;
* access document history.

### Collaborator

Responsible for creating and maintaining their documents and consulting approved documents.

Main capabilities:

* create documents;
* upload files;
* edit their own drafts;
* submit documents for review;
* view their own documents in any lifecycle status;
* view documents created by other users only while they are `APPROVED`.

---

## 6. MVP Scope

The first version of Docfy will include:

### Authentication

* user login;
* logout;
* session management.

### Document Management

* create documents;
* edit drafts;
* view documents;
* archive documents;
* basic document metadata.

### File Management

* upload document files;
* list file metadata for visible documents;
* download files from visible documents;
* validate allowed file types;
* validate maximum file size;
* keep binary content outside PostgreSQL.

Uploads use the same edit authorization as document metadata: files may be
attached only to an editable `DRAFT`. `ADMIN` and `MANAGER` can upload to any
draft, while `COLLABORATOR` can upload only to a draft they created. Listing
and download use the document visibility policy and preserve safe `404`
resource concealment.

The initial supported formats are PDF, UTF-8 text, PNG and JPEG. The backend
validates extension, declared content type and the supported signature/content
shape. Empty files and files over the configurable limit are rejected.

### Categories

Every document must reference one persisted category. The initial reference
data is:

* Meeting Minutes;
* Certificate;
* Contract;
* Notice;
* Regulation;
* Official Letter;
* Other.

Categories are listed through an authenticated read-only API. This MVP does
not provide administrative category CRUD.

Category is mutable only while the document is in `DRAFT`, through the same
metadata update operation and edit authorization policy used for title and
description.

### Document Workflow

Initial lifecycle:

```text
DRAFT
  ↓
IN_REVIEW
  ├── approve → APPROVED → ARCHIVED
  └── reject  → DRAFT
```

Rejection returns the document directly to `DRAFT`; it does not create a separate persisted state.

### Authorization

Initial roles:

* ADMIN;
* MANAGER;
* COLLABORATOR.

Document visibility is defined by role, creator and status:

| Role | Own document, any status | Other `DRAFT` | Other `IN_REVIEW` | Other `APPROVED` | Other `ARCHIVED` |
|---|---:|---:|---:|---:|---:|
| `ADMIN` | Yes | Yes | Yes | Yes | Yes |
| `MANAGER` | Yes | Yes | Yes | Yes | Yes |
| `COLLABORATOR` | Yes | No | No | Yes | No |

`COLLABORATOR` users can always view documents they created, regardless of status. For documents created by another user, they can view only the `APPROVED` status.

When a document exists but is not visible to the authenticated user, the API must return the same `404 Not Found` response used for a nonexistent document. This resource-concealment rule prevents document enumeration.

### Search and Filters

Users will be able to search and filter documents by:

* title;
* code;
* category;
* status.

Text search matches title or human-readable document code without regard to
letter case. Category and status are independent filters. When criteria are
combined, text search, category and status are joined with `AND`; within the
text criterion, title and document code are joined with `OR`.

All search and filter criteria apply only to documents visible to the
authenticated user and are evaluated before pagination.

---

## 7. Out of Scope — MVP

The following features will not be part of the initial MVP:

* electronic signatures;
* external customer accounts;
* OCR;
* AI document classification;
* integration with external storage providers;
* advanced notifications;
* advanced analytics;
* mobile native application;
* multi-tenant architecture.

These capabilities may be evaluated in future versions.

---

## 8. Functional Requirements

### FR-001 — Authentication

The system must allow registered users to authenticate using valid credentials.

### FR-002 — Document Creation

Authorized users must be able to create documents.

### FR-003 — Document Editing

Documents may be edited only while they are in `DRAFT` status.

`ADMIN` and `MANAGER` users may edit any draft document.

`COLLABORATOR` users may edit only draft documents they created.

The metadata update operation must support partial modification of title,
description and category without allowing clients to modify lifecycle status,
ownership or system-managed metadata.

### FR-004 — Document Submission

Users must be able to submit a document for review.

### FR-005 — Document Review

Authorized users must be able to review submitted documents.

### FR-006 — Document Approval

Managers and Administrators must be able to approve documents under review.

### FR-007 — Document Rejection

Managers and Administrators must be able to reject documents under review, returning them to `DRAFT`.

### FR-008 — Document Archive

Authorized users must be able to archive approved documents.

### FR-009 — Document Search

Users must be able to search documents they are authorized to access.

### FR-010 — Document Filtering

Users must be able to filter documents by category and status.

### FR-011 — File Upload

Users must be able to upload supported files to an editable draft, list file
metadata and download files belonging to documents they can view.

### FR-012 — Audit History

Relevant document lifecycle changes must be recorded as immutable audit events.

Each successful lifecycle transition must record:

* document identifier;
* actor;
* action;
* timestamp;
* previous status;
* new status;
* correlation ID when available.

The following lifecycle actions generate audit events:

* document submission;
* document approval;
* document rejection;
* document archive.

Audit information must be persisted in the same transaction as the lifecycle change.

If audit persistence fails, the lifecycle transition must also be rolled back.

`ADMIN` and `MANAGER` users may retrieve the audit history of documents they are authorized to view.

`COLLABORATOR` users cannot retrieve audit history.

When the requested document is not visible to the authenticated user, audit history requests must return the same `404 Not Found` response used for a nonexistent document.

---

## 9. Business Rules

### BR-001 — Unique Document Code

Every document has both a technical UUID and a unique, immutable human-readable code generated exclusively by the backend in the format `DOC-000001`.

Document code allocation uses a database sequence. It must remain safe under concurrent creation and must not derive the next value from `MAX(code)`.

### BR-002 — Required Information

A document must contain at least:

* title;
* category;
* responsible user.

The category must reference the persisted category catalog.

### BR-003 — Approval Permission

Only users with `MANAGER` or `ADMIN` roles can approve or reject documents.

### BR-004 — Document Submission

Documents in `DRAFT` status may be submitted for review.

`ADMIN` and `MANAGER` users may submit any draft document.

`COLLABORATOR` users may submit only draft documents they created.

A successful submission changes the document status from `DRAFT` to `IN_REVIEW`.

### BR-005 — Review Submission

Only documents in `DRAFT` status can be submitted for review.

`ADMIN` and `MANAGER` users can submit any draft document.

`COLLABORATOR` users can submit only draft documents they created.

### BR-006 — Approval State

Only documents in `IN_REVIEW` status can be approved or rejected.

Only users with `MANAGER` or `ADMIN` roles can approve or reject documents.

Rejection returns the document to `DRAFT`; there is no separate `REJECTED` status.

### BR-007 — Archive State

Only documents in `APPROVED` status can be archived.

Only users with `MANAGER` or `ADMIN` roles can archive approved documents.


### BR-008 — Document Archive

Only `MANAGER` and `ADMIN` users may archive documents.

Only documents in `APPROVED` status may be archived.

A successful archive operation changes the document status from `APPROVED` to `ARCHIVED`.


### BR-009 — Auditability

Every successful document lifecycle transition must create an immutable audit event.

Each audit event must record:

* document identifier;
* actor;
* action;
* timestamp;
* previous status;
* new status;
* correlation ID when available.

The supported audit actions are:

* `DOCUMENT_SUBMITTED`;
* `DOCUMENT_APPROVED`;
* `DOCUMENT_REJECTED`;
* `DOCUMENT_ARCHIVED`.

Lifecycle state changes and their corresponding audit events must be committed atomically.

A document status change must not be persisted if the corresponding audit event cannot be persisted.

Audit history access follows these rules:

* `ADMIN` users may view document audit history;
* `MANAGER` users may view document audit history;
* `COLLABORATOR` users cannot view document audit history;
* document visibility must be evaluated before audit authorization;
* a missing or concealed document produces `404 Not Found`;
* a visible document whose audit history is forbidden to the caller produces `403 Forbidden`.

### BR-010 — Access Control

Document read access must follow these rules:

* `ADMIN` and `MANAGER` can view every document, regardless of creator or status;
* `COLLABORATOR` can view every document they created, regardless of status;
* `COLLABORATOR` can view another user's document only when its status is `APPROVED`;
* a document that is missing or not visible to the authenticated user produces the same public `404 Not Found` response.

The same visibility policy must be applied to direct lookup, listing and search. Authorization filtering must occur before pagination or result disclosure.

Document edit authorization is evaluated independently from read visibility.

For metadata updates:

* `ADMIN` and `MANAGER` can edit any `DRAFT` document;
* `COLLABORATOR` can edit only `DRAFT` documents they created;
* documents in `IN_REVIEW`, `APPROVED` or `ARCHIVED` status cannot be edited directly by any role;
* if the authenticated user cannot view the target document, the API must return the same `404 Not Found` response used for a nonexistent document;
* if the authenticated user can view the target document but cannot edit it, the API must return `403 Forbidden`.

This distinction protects against document enumeration while preserving explicit authorization semantics for resources already visible to the user.

### BR-011 — Document Files

File upload follows `DocumentEditPolicy`; file listing and download follow
`DocumentVisibilityPolicy`. Missing or concealed documents and attachments
produce the same public `404 Not Found` contract.

The original filename is metadata only. Storage uses a backend-generated,
collision-resistant key and must never derive a physical path directly from a
client-provided filename. PostgreSQL stores metadata while binary content is
stored through the configured `FileStorage` implementation.

---


## 10. Main User Flows

### Document Creation

```text
Login
  ↓
Documents
  ↓
Create document
  ↓
Enter metadata
  ↓
Save as draft
Save as draft
  ↓
Upload file (optional)
```

### Document Approval

```text
Draft
  ↓
Submit for review
  ↓
Manager reviews
  ↓
Approve / Reject
  ↓
Status updated
  ↓
Audit event created
```

### Document Search

```text
Documents
  ↓
Search / Filters
  ↓
Results
  ↓
Document details
```

---

## 11. Quality Attributes

Quality requirements are considered part of the product and not only part of the testing phase.

The initial quality attributes include:

### Usability

Critical workflows should be clear, predictable and efficient.

### Accessibility

Critical user journeys should target WCAG 2.2 Level AA compliance.

### Performance

Common operations such as authentication, document listing and search should provide acceptable response times under expected workload.

### Reliability

The system should handle failures without causing data corruption or inconsistent document states.

### Security

Authentication, authorization and document access must follow least-privilege principles.

### Compatibility

The web application should support modern desktop browsers and responsive layouts.

### Maintainability

The application and automated test suites should be structured to support continuous evolution.

---

## 12. Testability Requirements

### TR-001 — Stable UI Identification

Critical interface elements must expose stable identifiers or accessible semantic attributes suitable for automated testing.

### TR-002 — Test Data

Test environments must support synthetic and reproducible test data.

### TR-003 — Environment Reset

Test data should be resettable to provide deterministic automated test execution.

### TR-004 — Health Check

The backend should expose a health endpoint for environment validation.

### TR-005 — Structured Errors

API errors should return predictable status codes and structured error responses.

### TR-006 — Observability

Relevant backend operations should generate structured logs.

### TR-007 — Correlation

Requests should support correlation identifiers to simplify failure investigation.

### TR-008 — Failure Simulation

Where feasible, the test environment should allow controlled simulation of failures such as timeouts and unavailable dependencies.

---

## 13. Quality Principles

Docfy follows the following principles:

* Quality is a shared responsibility.
* Quality starts before implementation.
* Testing should be driven by risk.
* Automation is a means, not the goal.
* Testability is a product requirement.
* Accessibility and usability are quality requirements.
* Production feedback contributes to product quality.
* AI can assist analysis, but humans remain responsible for decisions.

---

## 14. Initial Roadmap

### v0.1 — Product Foundation

* product definition;
* architecture definition;
* authentication;
* document CRUD;
* categories.

### v0.2 — Workflow

* review;
* approval;
* rejection;
* archive;
* audit trail.

### v0.3 — Authorization

* roles;
* permissions;
* access-control testing.

### v0.4 — Quality Engineering

* API automated tests;
* E2E automated tests;
* CI/CD;
* reports;
* quality gates.

### v0.5 — Non-functional Quality

* performance;
* accessibility;
* usability;
* compatibility;
* reliability.

### v0.6 — Observability

* structured logs;
* correlation IDs;
* monitoring;
* failure investigation.

### v0.7 — AI-assisted Quality

* AI-assisted requirement analysis;
* risk brainstorming;
* test idea generation;
* synthetic test data support;
* failure and log analysis experiments.

### v1.0 — Portfolio Release

Complete Quality Engineering case study with product implementation, automated tests, non-functional evaluations, CI/CD evidence, quality metrics and documented engineering decisions.

---

## 15. Document Evolution

This document represents the initial product hypothesis.

Requirements and business rules may evolve as new risks, usability findings and technical constraints are discovered.

Changes should be documented and traceable throughout the project lifecycle.
