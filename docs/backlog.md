# Backlog and 15-task plan

The board is frozen at 15 tasks for the MVP. Status describes repository readiness, not external work that has not been run.

| ID | User story / task | Acceptance / completion evidence | Status |
|---|---|---|---|
| T01 | As product owner, I need the user/pain-point and scope baseline so the team can agree what to build. | Stakeholders, constraints, objectives, measurable criteria and 15-task scope documented. | Done |
| T02 | As delivery team, I need traceable stories and a Definition of Done. | This backlog provides stories, acceptance criteria, task order and DoD. | Done |
| T03 | As developer, I need a reproducible Java/Maven project so I can build locally. | Java 25/Spring Boot/Maven structure and local setup documented. | Implemented |
| T04 | As agent, I need to create and view a validated activation record. | Create API/UI, generated identifier/timestamps, validation and duplicate handling. | Implemented |
| T05 | As agent, I need to edit drafts and find records quickly. | Draft-only update; combined partial search and status filter. | Implemented |
| T06 | As operations staff, I need role-owned status transitions. | Agent submit, Supervisor review/reject, Admin activate; unauthorized transitions denied. | Implemented |
| T07 | As operations lead, I need a current workload summary. | Dashboard counts all statuses, including zero values. | Implemented |
| T08 | As QA, I need regression coverage for core user journeys. | MockMvc tests and four Selenium journeys with failure screenshot support. | Implemented; 4 Selenium journeys passed locally |
| T09 | As contributor, I need Git/GitHub collaboration conventions. | Ignore rules, issue/PR templates, branch and review policy. | Remote, `develop`, issue #1 and templates present; GitHub branch protection still unconfigured |
| T10 | As reviewer, I need a pull-request-based feature/release workflow. | Branch/commit/PR procedure documented. | Feature branch pushed; PR #2 targets `develop`; independent review and merge pending |
| T11 | As developer, I need CI to test, package and archive the application. | Jenkinsfile includes checkout, Maven tests, package and JAR archive. | Local Jenkins 2.580.2 multibranch scan succeeded; feature build #1 passed and archived the JAR. `develop` #1 passed. Pre-merge `main` #1 failed because its older Selenium configuration used occupied port 18080; rerun `main` after merging the feature fix. |
| T12 | As QA, I need browser tests to gate release/deployment. | All-branch Selenium stage publishes JUnit reports and failure screenshots; failed journeys stop deployment. | Feature build #1 passed 4 unit tests and 4 Selenium journeys; a prior stale-element failure was fixed and rerun. Main-branch Selenium remains blocked by its pre-merge port setting. |
| T13 | As operator, I need a versioned container artifact. | Non-root Dockerfile; pipeline tags by Jenkins build number. | Image built locally; container lifecycle and health check passed |
| T14 | As platform engineer, I need repeatable host prerequisites and app configuration. | Ansible playbook and localhost inventory cover Docker, user, directory, container. | Docker Desktop-backed Linux runner provisioned the app user/group and directory and deployed the container. Native Docker package/service provisioning still requires an authorized Linux VM. |
| T15 | As operator, I need health, idempotency, and rollback/recovery steps. | Runbook describes second apply, HTTP health check and prior-image redeploy. | Ansible rerun reported `changed=0`; rollback to `sim-activation:0.1.0-local` reported `changed=1`; health returned `{"status":"UP"}` on port 18082. Full Linux-host recovery remains pending an authorized VM. |

## Planned increments

| Sprint | Goal | Tasks |
|---|---|---|
| Sprint 1 | Agree and ship the record core | T01–T05 |
| Sprint 2 | Complete workflow and verify user journeys | T06–T08 |
| Sprint 3 | Establish collaboration, CI, and container release path | T09–T13 |
| Sprint 4 | Reproducible host setup and recovery evidence | T14–T15 |

## Story-level acceptance tests

- **Agent:** Given a valid draft, when I search by a partial customer name or identifier, then the matching record is returned; when I edit it as `AGENT`, the new values are persisted.
- **Supervisor:** Given a submitted record, when I move it to review as `SUPERVISOR`, then it appears as `IN_REVIEW`; when it is in review, I can reject it.
- **Administrator:** Given a reviewed record, when I activate it as `ADMIN`, then its state becomes `ACTIVATED`; another role cannot perform that transition.
- **Operations lead:** Given records in any mix of states, when I open the dashboard, then it displays a count for each of the five states.
- **QA/operator:** Given an invalid build or a failed Selenium journey, when the main pipeline runs, then deployment does not proceed and reports are retained.
