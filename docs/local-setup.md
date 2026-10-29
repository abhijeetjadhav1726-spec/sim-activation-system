# Local setup and operations runbook

## Prerequisites

- Java 25 (LTS) and Maven 3.9+ for `mvn spring-boot:run` / `mvn clean verify`.
- Docker Desktop/Engine for image and container commands.
- Chrome or Chromium for Selenium browser tests; Jenkins agents need a compatible browser and driver resolution.
- An Ubuntu/Debian host, Ansible, and the `community.docker` collection for the sample provisioning playbook.

Install the Ansible collection once:

```powershell
ansible-galaxy collection install -r ansible/requirements.yml
```

## Ansible provisioning

The sample inventory targets localhost and uses `become`, so it is intended for a disposable Linux VM with sudo, not Windows:

```sh
export SIM_ACTIVATION_IMAGE=sim-activation:0.1.0
ansible-playbook -i ansible/hosts.ini ansible/site.yml --check --diff
ansible-playbook -i ansible/hosts.ini ansible/site.yml
ansible-playbook -i ansible/hosts.ini ansible/site.yml
curl --fail http://127.0.0.1:8080/actuator/health
```

The second successful apply should report no changes (idempotency). For a remote host, update the inventory and use a registry-qualified image available to that host. The playbook uses the `community.docker.docker_container` module and assumes a package named `docker.io`, suitable for Debian-family hosts; adjust packages for other systems.

### Windows workstation with Docker Desktop

This local training option runs Ansible in a disposable Linux container and connects it to Docker Desktop's engine. The mounted Docker socket grants the runner administrator-equivalent control of the local Docker engine; use it only with this trusted playbook. It does not configure Windows itself or replace a real Linux target for package/service provisioning.

Build the application image first if the selected tag is not already present. Then, from PowerShell at the repository root, run:

```powershell
$repoPath = (Resolve-Path .).Path
docker run --rm `
  -v "${repoPath}:/workspace" `
  -v /var/run/docker.sock:/var/run/docker.sock `
  -w /workspace `
  -e SIM_ACTIVATION_IMAGE=sim-activation:0.1.0 `
  python:3.12-slim sh -lc "pip install --no-cache-dir ansible-core docker && ansible-galaxy collection install -r ansible/requirements.yml && ansible-playbook -i ansible/hosts.ini ansible/site.yml -e docker_runtime_managed=false -e app_port=18082 -e app_health_url=http://host.docker.internal:18082/actuator/health && ansible-playbook -i ansible/hosts.ini ansible/site.yml -e docker_runtime_managed=false -e app_port=18082 -e app_health_url=http://host.docker.internal:18082/actuator/health && SIM_ACTIVATION_IMAGE=sim-activation:0.1.0-local ansible-playbook -i ansible/hosts.ini ansible/site.yml -e docker_runtime_managed=false -e app_port=18082 -e app_health_url=http://host.docker.internal:18082/actuator/health"
```

In this mode, `docker_runtime_managed=false` skips Linux package installation and service management because Docker Desktop supplies the daemon. The playbook still ensures the app user/group and directory in the ephemeral Linux runner, creates the application container through the mounted engine, and checks its health at the host-mapped port. The command applies the same image twice in the same runner to demonstrate idempotency, then switches to the prior `0.1.0-local` image to demonstrate recovery. For full package, user, service, and recovery validation on a Linux node, use the standard Ansible commands above against an authorized disposable VM.

Local execution evidence: the first apply completed with `changed=4` and two runtime tasks skipped; the second apply completed with `changed=0`; switching to `sim-activation:0.1.0-local` completed with `changed=1`. The health task returned HTTP 200 on each apply after startup retries. The final container is `sim-activation` on port 18082 and returned `{"status":"UP"}`. The Ansible runner was removed when the command finished; the application container remains available locally.

## Health and rollback

Health evidence is the HTTP 200 response from `/actuator/health`. Record the current image tag before release. To recover manually on the Docker host, replace `PREVIOUS_TAG` with the last known-good version:

```sh
docker pull registry.example/sim-activation:PREVIOUS_TAG
docker rm -f sim-activation || true
docker run -d --restart unless-stopped --name sim-activation \
  -p 8080:8080 -v sim-activation-data:/app/data \
  registry.example/sim-activation:PREVIOUS_TAG
curl --fail http://127.0.0.1:8080/actuator/health
```

For Ansible recovery, set `SIM_ACTIVATION_IMAGE` to the previous immutable tag and rerun the playbook. The database is retained in the named volume; schema and data rollback are not implemented. A production release must define database backup/migration and rollback policy before deployment.
