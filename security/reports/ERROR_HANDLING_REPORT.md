# ERROR_HANDLING Security Report

## Status: LOW

## Findings

- `JsonApiServlet.service` catches every exception, returns a generic message and logs the details server-side. `web.xml` maps 403/404/500 to friendly JSPs. Live: error pages don't leak stack traces (PASS).
- **Swagger UI and the spec are public:** `/api-docs/index.html`, `/api-docs/swagger.json` and `/api/docs/*` are whitelisted in `SessionAuthFilter.isPublicPath`. Live scan FAIL on :8085 and through :5173. The repo is public, so the spec is public anyway and the impact is low.
- Some DAO catch blocks only call `System.err.println(e.getMessage())` (e.g. `PasswordResetTokenDAO`). Clients see nothing, but the logs lose the stack trace.
- No Spring Actuator or debug endpoints. Nest uses its default exception filter (no stack traces in production).

## What's at risk

An attacker gets a full map of the API (it's already in the public repo).

## What's already secure

Global handler, generic messages, friendly error pages.

## Recommendations

Put `/api-docs` and `/api/docs` behind login (remove them from `isPublicPath`), or leave them out of production builds.

## Verification result

Not changed.
