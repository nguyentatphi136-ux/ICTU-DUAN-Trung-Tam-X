# DEPENDENCIES Security Report

## Status: LOW

## Findings

- `npm audit --omit=dev`: 0 vulnerabilities in `frontend`, `backend-nest` and `backend/legacy_node_be`.
- `backend-nest` dev dependencies: `braces` (high, stack-exhaustion DoS, GHSA-vfj7-8cjw-p6xm) and `sprintf-js` (moderate). Build-time only and not shipped (the Dockerfile uses `npm ci --omit=dev`).
- Maven: Tomcat embed 9.0.108, mysql-connector-j 8.3.0, gson 2.10.1, jbcrypt 0.4, poi-ooxml 5.2.5, javax.mail 1.6.2, jstl 1.2. All legitimate, well-known artifacts. **No Maven CVE scanner was run** (OWASP dependency-check isn't configured).
- Every npm package has a committed lock file. Versions use `^` ranges, which is normal with lock files.
- `semgrep` and `gitleaks` aren't installed.

## What's at risk

Low: a build-tool DoS only.

## What's already secure

Lock files committed, production audit clean, Docker image leaves out dev dependencies.

## Recommendations

1. `cd backend-nest && npm audit fix` (try without `--force` first).
2. Run `mvn org.owasp:dependency-check-maven:check` now and then, and keep Tomcat 9 on the latest patch.
3. Optional: install `gitleaks` and `semgrep`, e.g. in CI.

## Verification result

No change.
