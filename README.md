# SIM Activation System

A small, locally runnable SIM activation workflow MVP for DevOps training. It supports activation record creation, viewing, editing of drafts, combined search/filtering, a role-checked status workflow, and a status summary dashboard.

## Technology

- Java 25 (LTS), Spring Boot 3.5, Maven
- Spring MVC REST API and static HTML UI
- Spring Data JPA with H2 file-backed local database
- Embedded Tomcat on port 8080
- JUnit/MockMvc tests and optional Selenium/Chrome journeys
- Docker, Jenkins Pipeline, Ansible

## Run locally

Install a JDK 25 distribution and Maven 3.9 or newer, then from the repository root:

```powershell
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080). The local database is stored under `data/`; do not put real customer or SIM data in this training app. Stop the server with `Ctrl+C`.

Run tests and package the executable JAR:

```powershell
mvn clean verify
mvn spring-boot:run
```

If Java/Maven are not installed locally but Docker is available, run the build in a disposable Maven container:

```powershell
docker run --rm -v "${PWD}:/workspace" -w /workspace maven:3.9.16-eclipse-temurin-25 mvn -B clean verify
```

Run the Selenium browser suite (Chrome/Chromium must be installed; Selenium Manager resolves a compatible driver):

```powershell
mvn -Pselenium verify
```

JUnit XML reports are under `target/surefire-reports/` and `target/failsafe-reports/`. Browser failure screenshots are written to `target/selenium-failures/`.

## API

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/activations` | Create a draft (`customerName`, `msisdn`, `iccid`) |
| `GET` | `/api/activations/{id}` | View a record |
| `GET` | `/api/activations?query=...&status=...` | Search by customer/MSISDN/ICCID and optionally filter by status |
| `PUT` | `/api/activations/{id}` | Update a draft; requires `X-Actor-Role: AGENT` |
| `POST` | `/api/activations/{id}/status` | Change status with JSON `{"status":"..."}` and `X-Actor-Role` |
| `GET` | `/api/dashboard` | Counts for every status |
| `GET` | `/actuator/health` | Deployment health probe |

MSISDN must be 10–15 digits, ICCID 18–22 digits, and both are unique. Workflow:

```text
DRAFT --AGENT--> SUBMITTED --SUPERVISOR--> IN_REVIEW
IN_REVIEW --ADMIN--> ACTIVATED
IN_REVIEW --SUPERVISOR--> REJECTED
```

The UI role selector and `X-Actor-Role` header are **training-only authorization simulation**, not authentication. A caller can forge the header. Do not use real personal data or expose this app to untrusted networks. A production system must authenticate users and derive roles from a trusted identity provider, add audit history, use an appropriately secured production database, and protect sensitive data.

## Docker

Build and run locally:

```powershell
docker build -t sim-activation:0.1.0 .
docker run --rm --name sim-activation -p 18080:8080 -v sim-activation-data:/app/data sim-activation:0.1.0
```

Or start it with Compose:

```powershell
$env:APP_PORT = '18080'
docker compose up --build
```

Open [http://localhost:18080](http://localhost:18080). Port 18080 avoids the default Jenkins port 8080; set `APP_PORT` to another free host port if needed.

The application uses embedded Tomcat. A production host should place a managed reverse proxy such as Nginx in front of the container and terminate TLS there.

## CI/CD and configuration

- [Jenkinsfile](./Jenkinsfile) checks out, tests, packages, archives the JAR, and runs Selenium on every branch. On `main`, it also builds a versioned image and deploys a fresh local container on port 18080 after tests pass. Publishing and remote deployment are opt-in Jenkins parameters; configure the credentials and agent prerequisites in [docs/devops-workflow.md](./docs/devops-workflow.md) first.
- For a native Windows Jenkins installation, follow [windows-jenkins-setup.md](./docs/windows-jenkins-setup.md). The pipeline uses Windows batch/PowerShell steps.
- [Dockerfile](./Dockerfile) builds a non-root runtime image.
- [Ansible playbook](./ansible/site.yml) installs Docker, creates the app account and directory, runs the requested image, and probes the health endpoint. See its requirements and idempotency/rollback procedure in [docs/local-setup.md](./docs/local-setup.md).
- Requirements, diagrams, stakeholder context and the 15-task backlog are in [docs/requirements.md](./docs/requirements.md), [docs/architecture.md](./docs/architecture.md), [docs/backlog.md](./docs/backlog.md), and [docs/devops-workflow.md](./docs/devops-workflow.md).

## Repository collaboration

This checkout is connected to `https://github.com/abhijeetjadhav1726-spec/sim-activation-system.git`. Issue and pull-request templates are present under `.github/`, and [CONTRIBUTING.md](./CONTRIBUTING.md) documents branch names and review rules. Configure and maintain branch protection through GitHub repository settings; never store credentials in this repository.
