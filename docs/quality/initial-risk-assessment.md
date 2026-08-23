# Docfy — Initial Quality Risk Assessment

**Version:** 0.1
**Status:** Draft
**Scope:** MVP
**Related document:** [Product Definition](../product/product-definition.md)

---

## 1. Purpose

This document identifies and prioritizes the initial product quality risks for the Docfy MVP.

The goal is not to create an exhaustive list of possible defects.

The goal is to identify the failures that could cause the highest impact to users, business rules, data integrity or product reliability and use this information to guide:

* test strategy;
* automation priorities;
* exploratory testing;
* non-functional testing;
* quality gates;
* release decisions.

Testing effort should be proportional to risk.

---

## 2. Risk-Based Testing Approach

Each identified risk is evaluated using two dimensions:

### Impact

Represents the consequence if the risk becomes a real defect.

| Level    | Description                                                                                                                  |
| -------- | ---------------------------------------------------------------------------------------------------------------------------- |
| Critical | May expose protected data, violate authorization rules, corrupt important information or break a critical business workflow. |
| High     | Causes significant product malfunction or prevents an important user journey from being completed.                           |
| Medium   | Causes partial degradation, incorrect behavior or user confusion with an available workaround.                               |
| Low      | Causes limited inconvenience with minimal impact on the main product workflows.                                              |

### Probability

Represents the likelihood of the failure occurring based on workflow complexity, dependencies, state transitions and implementation characteristics.

| Level  | Description                                                                |
| ------ | -------------------------------------------------------------------------- |
| High   | Failure is considered likely due to complexity or frequent execution.      |
| Medium | Failure is reasonably possible under normal or edge-case conditions.       |
| Low    | Failure requires uncommon conditions or has low implementation complexity. |

---

## 3. Priority Model

Risk priority is derived from the combination of impact and probability.

| Impact   | Low Probability | Medium Probability | High Probability |
| -------- | --------------- | ------------------ | ---------------- |
| Critical | High            | Critical           | Critical         |
| High     | Medium          | High               | Critical         |
| Medium   | Low             | Medium             | High             |
| Low      | Low             | Low                | Medium           |

Critical and High risks should receive earlier and deeper validation.

---

# 4. Initial Risk Register

## RISK-001 — Unauthorized document access

**Related requirements:** BR-010, FR-009
**Impact:** Critical
**Probability:** Medium
**Priority:** Critical

### Risk

A user may access a document for which they do not have permission.

### Potential impact

* confidential information exposure;
* access-control violation;
* privacy incident;
* unauthorized download or visualization of documents.

### Recommended coverage

* API authorization tests;
* role-based access tests;
* direct resource access attempts;
* E2E permission scenarios;
* negative testing;
* security-focused exploratory testing.

---

## RISK-002 — Unauthorized document approval or rejection

**Related requirements:** BR-003, FR-006, FR-007
**Impact:** Critical
**Probability:** Medium
**Priority:** Critical

### Risk

A `COLLABORATOR` or another unauthorized user may approve or reject a document.

### Potential impact

* business workflow violation;
* unauthorized decision;
* invalid document status;
* loss of trust in the approval process.

### Recommended coverage

* API authorization tests;
* role matrix validation;
* E2E tests using multiple user profiles;
* direct endpoint access attempts;
* regression automation.

---

## RISK-003 — Invalid document state transition

**Related requirements:** BR-005, BR-006, BR-007
**Impact:** High
**Probability:** Medium
**Priority:** High

### Risk

The application may allow invalid state transitions.

Examples:

```text
DRAFT → APPROVED

ARCHIVED → IN_REVIEW

APPROVED → DRAFT

DRAFT → ARCHIVED
```

### Potential impact

* inconsistent workflow;
* invalid document lifecycle;
* inaccurate audit history;
* difficult recovery.

### Recommended coverage

* state transition testing;
* API tests;
* decision-table testing;
* negative scenarios;
* E2E critical workflow coverage.

---

## RISK-004 — Approved document can be modified directly

**Related requirements:** BR-004, FR-003
**Impact:** High
**Probability:** Medium
**Priority:** High

### Risk

An approved document may be modified without generating a controlled new version.

### Potential impact

* approved information changed without review;
* loss of document integrity;
* audit inconsistency;
* regulatory or operational risk.

### Recommended coverage

* API negative tests;
* authorization and state validation;
* direct request manipulation;
* E2E regression;
* database validation.

---

## RISK-005 — Audit history is incomplete or incorrect

**Related requirements:** FR-012, BR-009
**Impact:** High
**Probability:** Medium
**Priority:** High

### Risk

Relevant lifecycle changes may not generate an accurate audit record.

### Potential impact

* inability to identify who performed an action;
* missing previous or new status;
* incorrect timestamps;
* reduced traceability;
* difficulty investigating incidents.

### Recommended coverage

* API tests;
* database validation;
* workflow tests;
* audit consistency checks;
* exploratory testing.

Expected minimum audit information:

```text
user
action
timestamp
previous status
new status
```

---

## RISK-006 — Duplicate document identifiers

**Related requirement:** BR-001
**Impact:** High
**Probability:** Low
**Priority:** Medium

### Risk

Two or more documents may receive the same unique identifier.

### Potential impact

* incorrect document retrieval;
* data integrity problems;
* ambiguous audit information;
* incorrect updates or references.

### Recommended coverage

* API tests;
* database constraints;
* concurrency tests where appropriate;
* duplicate data scenarios.

---

## RISK-007 — Invalid or unsafe file upload

**Related requirements:** FR-011, BR-008
**Impact:** High
**Probability:** Medium
**Priority:** High

### Risk

The application may accept unsupported, oversized or potentially unsafe files.

### Potential impact

* application instability;
* storage misuse;
* security exposure;
* poor user experience;
* upload processing failures.

### Recommended coverage

* supported file types;
* unsupported extensions;
* MIME type validation;
* maximum file size boundaries;
* empty files;
* corrupted files;
* renamed extensions;
* API and E2E tests.

---

## RISK-008 — Authentication or session validation failure

**Related requirement:** FR-001
**Impact:** Critical
**Probability:** Medium
**Priority:** Critical

### Risk

Authentication or session handling may allow unauthorized application access or incorrect session behavior.

### Potential impact

* unauthorized system access;
* account exposure;
* stale session usage;
* incorrect logout behavior.

### Recommended coverage

* valid authentication;
* invalid credentials;
* expired session;
* missing authentication;
* invalid or manipulated tokens;
* logout;
* access after logout;
* API + E2E regression.

---

## RISK-009 — Search exposes unauthorized documents

**Related requirements:** FR-009, BR-010
**Impact:** Critical
**Probability:** Medium
**Priority:** Critical

### Risk

Search results may return documents the authenticated user is not allowed to access.

### Potential impact

Even when the document itself cannot be opened, metadata may expose:

* document title;
* category;
* owner;
* status;
* identifiers.

### Recommended coverage

* API authorization tests;
* search using users with different permissions;
* filter combinations;
* direct document access after search;
* E2E validation.

---

## RISK-010 — Concurrent actions create inconsistent document state

**Related requirements:** FR-004, FR-006, FR-007, BR-006
**Impact:** High
**Probability:** Low
**Priority:** Medium

### Risk

Two users may perform conflicting actions on the same document at approximately the same time.

Example:

```text
Manager A → APPROVE

Manager B → REJECT
```

### Potential impact

* inconsistent final state;
* duplicated audit entries;
* lost updates;
* incorrect workflow result.

### Recommended coverage

* API concurrency tests;
* database consistency validation;
* exploratory testing;
* reliability testing.

---

## RISK-011 — Required document information is not validated

**Related requirement:** BR-002
**Impact:** Medium
**Probability:** Medium
**Priority:** Medium

### Risk

Documents may be created without mandatory information.

Required initial information:

* title;
* category;
* responsible user.

### Potential impact

* incomplete records;
* search and classification problems;
* unclear ownership;
* workflow problems.

### Recommended coverage

* API validation;
* E2E form validation;
* boundary and negative scenarios;
* database validation.

---

## RISK-012 — Document workflow loses data during failure

**Related quality attribute:** Reliability
**Related requirements:** TR-005, TR-006, TR-007
**Impact:** High
**Probability:** Low
**Priority:** Medium

### Risk

A timeout, service failure or interrupted request may partially update a document.

Example:

```text
status changed
      ↓
audit event fails
```

The system may finish with inconsistent information.

### Potential impact

* data corruption;
* status/audit mismatch;
* unclear operation result;
* difficult incident investigation.

### Recommended coverage

* reliability testing;
* controlled failure simulation;
* API testing;
* database validation;
* log analysis;
* correlation ID validation.

---

## RISK-013 — Poor document search performance

**Related quality attribute:** Performance
**Impact:** Medium
**Probability:** Medium
**Priority:** Medium

### Risk

Document search or listing may become slow as the amount of stored data increases.

### Potential impact

* degraded user experience;
* reduced productivity;
* timeouts;
* increased backend resource usage.

### Recommended coverage

* baseline performance tests;
* load testing;
* response-time monitoring;
* dataset growth scenarios;
* search/filter performance comparison.

Initial performance thresholds will be defined after architecture and workload assumptions are established.

---

## RISK-014 — Critical workflows are inaccessible

**Related quality attribute:** Accessibility
**Impact:** High
**Probability:** Medium
**Priority:** High

### Risk

Users relying on keyboard navigation, assistive technology or accessible semantics may be unable to complete critical workflows.

Critical journeys include:

* authentication;
* document creation;
* document search;
* review;
* approval.

### Recommended coverage

* keyboard navigation;
* focus order;
* accessible names;
* form labels;
* error announcements;
* semantic structure;
* contrast;
* automated accessibility checks;
* manual accessibility evaluation.

Target:

**WCAG 2.2 Level AA**

---

## RISK-015 — Workflow is technically correct but difficult to use

**Related quality attribute:** Usability
**Impact:** Medium
**Probability:** Medium
**Priority:** Medium

### Risk

Users may be able to complete a workflow technically while still having difficulty understanding what to do.

Examples:

* unclear document status;
* confusing approval actions;
* unclear error messages;
* insufficient system feedback;
* difficulty locating documents.

### Recommended coverage

* heuristic evaluation;
* exploratory testing;
* critical-task evaluation;
* error recovery analysis;
* workflow observation;
* usability findings and recommendations.

---

# 5. Risk Summary

| ID       | Risk                               | Impact   | Probability | Priority    |
| -------- | ---------------------------------- | -------- | ----------- | ----------- |
| RISK-001 | Unauthorized document access       | Critical | Medium      | 🔴 Critical |
| RISK-002 | Unauthorized approval/rejection    | Critical | Medium      | 🔴 Critical |
| RISK-003 | Invalid state transition           | High     | Medium      | 🟠 High     |
| RISK-004 | Approved document modification     | High     | Medium      | 🟠 High     |
| RISK-005 | Incorrect audit history            | High     | Medium      | 🟠 High     |
| RISK-006 | Duplicate document identifiers     | High     | Low         | 🟡 Medium   |
| RISK-007 | Invalid or unsafe upload           | High     | Medium      | 🟠 High     |
| RISK-008 | Authentication/session failure     | Critical | Medium      | 🔴 Critical |
| RISK-009 | Search exposes protected documents | Critical | Medium      | 🔴 Critical |
| RISK-010 | Concurrent workflow conflict       | High     | Low         | 🟡 Medium   |
| RISK-011 | Missing required information       | Medium   | Medium      | 🟡 Medium   |
| RISK-012 | Partial update during failure      | High     | Low         | 🟡 Medium   |
| RISK-013 | Search performance degradation     | Medium   | Medium      | 🟡 Medium   |
| RISK-014 | Inaccessible critical workflows    | High     | Medium      | 🟠 High     |
| RISK-015 | Poor workflow usability            | Medium   | Medium      | 🟡 Medium   |

---

# 6. Initial Testing Priority

Based on the current risk assessment, the first test efforts should prioritize:

### Priority 1 — Access and Authorization

```text
RISK-001
RISK-002
RISK-008
RISK-009
```

Focus:

* authentication;
* authorization;
* resource access;
* role validation.

### Priority 2 — Document Lifecycle Integrity

```text
RISK-003
RISK-004
RISK-005
RISK-007
```

Focus:

* state transitions;
* approved-document integrity;
* auditability;
* file validation.

### Priority 3 — Product Quality

```text
RISK-014
RISK-015
RISK-013
```

Focus:

* accessibility;
* usability;
* performance.

### Priority 4 — Reliability and Edge Conditions

```text
RISK-006
RISK-010
RISK-011
RISK-012
```

Focus:

* data integrity;
* concurrency;
* failure recovery;
* negative scenarios.

---

# 7. Testing Layers

The identified risks should not be validated exclusively through E2E testing.

Coverage should be distributed across the most appropriate layers.

```text
                       E2E
                        ▲
                   Integration
                        ▲
                       API
                        ▲
                 Component / Unit
```

Additional quality activities include:

```text
Exploratory Testing
Accessibility Evaluation
Usability Evaluation
Performance Testing
Reliability Testing
Security-focused Testing
Database Validation
```

The objective is to obtain fast feedback at lower testing layers while reserving E2E tests for critical user journeys.

---

# 8. Traceability

The project should progressively maintain traceability between:

```text
Requirement
      ↓
Risk
      ↓
Test Scenario
      ↓
Automated / Manual Test
      ↓
Execution Evidence
      ↓
Quality Decision
```

Example:

```text
BR-003
   ↓
RISK-002
   ↓
Unauthorized approval scenarios
   ↓
API + E2E
   ↓
CI execution
   ↓
Release evidence
```

A formal traceability matrix may be introduced as the test suite grows.

---

# 9. AI-Assisted Risk Analysis

Artificial Intelligence may be used as a supporting tool to brainstorm additional risks, edge cases and failure hypotheses.

The workflow should remain:

```text
Product requirements
        ↓
Risk analysis
        ↓
AI-assisted brainstorming
        ↓
QA review
        ↓
Validated risk register
```

AI-generated suggestions must not automatically become product requirements or confirmed risks.

They must be evaluated against:

* documented requirements;
* business rules;
* architecture;
* product context;
* engineering evidence.

**AI suggestions are hypotheses, not evidence.**

---

# 10. Risk Review

This risk assessment is expected to evolve.

It should be reviewed when:

* new functionality is introduced;
* business rules change;
* architecture changes;
* defects reveal previously unknown risks;
* production or test incidents occur;
* performance characteristics change;
* accessibility or usability findings reveal new product risks.

Risk-Based Testing is therefore treated as a continuous activity rather than a one-time planning exercise.
