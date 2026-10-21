# SIM Activation System — problem and requirements

## Problem statement

SIM activation requests need to move from intake to an auditable decision without losing track of customer/SIM identifiers or the person responsible for the next step. A small operations team needs a shared searchable view, clear status ownership, and a quick summary of pending and completed work. The MVP replaces an error-prone spreadsheet/manual hand-off with a single record service and a visible role-ordered workflow. This is a training scenario, not a carrier-grade activation platform.

## Users and stakeholders

| Stakeholder | Need / responsibility |
|---|---|
| Activation agent | Capture and correct draft requests; submit complete records |
| Supervisor / reviewer | Pick up submitted requests, review them, and reject invalid cases |
| Activation administrator | Approve reviewed requests and mark activation complete |
| Operations lead | See queue counts and identify bottlenecks |
| Product owner | Prioritize scope and accept the release |
| DevOps / platform engineer | Build, test, deploy, configure, observe, and recover the service |
| Security / privacy representative | Define identity, retention, audit, and data-protection controls before production |

## Current pain points and constraints

- Manual re-entry and disconnected status tracking make errors and hand-off delays hard to spot.
- Teams need fast lookup by customer, phone number, or SIM identifier.
- The MVP is limited to record workflow and summary; it does not connect to a carrier, verify identity, provision a network, send notifications, or process payments.
- No real personal or SIM data, production credentials, or external infrastructure are available in this workspace.
- The role header is a local training stub, not an authentication boundary.
- Java 17/Maven and a local H2 database keep the example inexpensive to run; a production data store and identity provider are out of MVP scope.

## Objectives and success measures

1. A valid request can be created and read through the UI/API; invalid identifiers are rejected.
2. Agents can edit drafts only; a role can perform only its permitted status transition.
3. Search matches customer name, MSISDN, or ICCID, and status filtering can be combined with search.
4. Dashboard counts always include all five statuses, including zero counts.
5. `mvn clean verify` succeeds and the four browser journeys cover create/read, search, draft update, and role-checked progression.
6. A build produces a runnable JAR/container and the health endpoint returns HTTP 200.

## MVP scope — frozen at 15 tasks

The requester-selected brief is treated as the scope baseline. Approval of the production deployment/security posture remains a stakeholder gate.

### Included

- Activation record create, view, update draft, search and status filtering.
- Agent, Supervisor, and Admin status workflow including activation and rejection.
- Summary dashboard, validation, duplicate protection, API, responsive local UI.
- Maven build, automated tests, browser test plan, Docker image, Jenkins pipeline-as-code, Ansible host configuration, local setup/release procedures.
- GitHub issue/PR templates and collaboration rules.

### Excluded

- Carrier/network provisioning, customer identity verification, OTP, billing, document capture, notifications, multi-tenant access, production authentication/authorization, audit trail, encryption/key management, high availability, and production data migration.
- Creating a GitHub account/repository, installing/configuring a live Jenkins server, pushing to Docker Hub, provisioning a remote host, or claiming deployment evidence without authorized infrastructure.

## Acceptance criteria

- **AC-01 Create:** Valid name/MSISDN/ICCID creates a `DRAFT`; duplicate MSISDN or ICCID returns conflict.
- **AC-02 Validate:** Blank name, non-digit identifier, out-of-range length, or invalid status request is rejected with a client error.
- **AC-03 Read/search:** View by ID; partial case-insensitive name and partial numeric-ID searches work; optional status filter composes with search.
- **AC-04 Update:** Only `AGENT` may edit a `DRAFT`; no record in a later state can be edited.
- **AC-05 Workflow:** `AGENT: DRAFT→SUBMITTED`; `SUPERVISOR: SUBMITTED→IN_REVIEW`; `ADMIN: IN_REVIEW→ACTIVATED`; `SUPERVISOR: IN_REVIEW→REJECTED`. Other roles/transitions fail without changing data.
- **AC-06 Dashboard:** Every status appears with a numeric count, including zero.
- **AC-07 Delivery:** Unit/API tests, Selenium report/failure screenshot mechanism, runnable container, Jenkins pipeline source, Ansible playbook and health probe are documented.

## SRS summary

| ID | Requirement |
|---|---|
| FR-01 | Create and view activation records with generated ID and timestamps. |
| FR-02 | Update record details only while in `DRAFT`, with agent permission. |
| FR-03 | Search by customer name, MSISDN, or ICCID; optionally filter by workflow status. |
| FR-04 | Enforce role-authorized forward transitions and a reviewer rejection path. |
| FR-05 | Return dashboard totals for all status values. |
| NFR-01 | Validate request fields and return readable 4xx errors for invalid/conflicting operations. |
| NFR-02 | Provide a local setup requiring Java 17/Maven or Docker; expose a health endpoint. |
| NFR-03 | Keep the sample non-production: no real customer data or embedded secrets. |

## Definition of Done

- Acceptance criteria are met and linked to a backlog task.
- Code follows the project conventions and includes focused automated tests.
- `mvn -B clean verify` passes; Selenium is run when browser/UI/workflow changes are made.
- API/UI behavior and setup docs are updated; no secrets or sensitive test data are committed.
- Pull request is reviewed, CI passes, and the change is merged to `develop`; production-only gates are explicitly not claimed as complete.
