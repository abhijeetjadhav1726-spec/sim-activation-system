# Contributing

## Branches

- `main` is the releasable branch.
- `develop` is the shared integration branch.
- Use `feature/<short-description>`, `bugfix/<short-description>`, or `chore/<short-description>` from `develop`.
- Use `hotfix/<short-description>` from `main`, then merge to both `main` and `develop`.
- Keep branch names lowercase and hyphen-separated.

## Pull requests

Open a pull request into `develop` for normal work. Include the related issue, a concise change summary, test evidence, and screenshots for UI changes. Require a successful `mvn -B clean verify`, at least one review, and resolution of all conversations before merging. Release from `main` with a `vMAJOR.MINOR.PATCH` tag.

## Commit messages

Use imperative, scoped messages, for example `feat: add activation search` or `test: cover role transition rules`. Keep unrelated changes in separate commits.

## GitHub setup

Create a private GitHub repository named `sim-activation-system`, add this workspace as its local source, and configure the remote only after confirming the correct owner and access policy. Protect `main` and `develop`; require pull requests and passing CI. Never commit real SIM/customer data, secrets, Jenkins credentials, or `.env` files.
