# Local setup and operations runbook

## Prerequisites

- Java 17 and Maven 3.9+ for `mvn spring-boot:run` / `mvn clean verify`.
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
