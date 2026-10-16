# Security Audit Summary

Date: 2026-10-16

Scope: `backend/` (Java Servlet/JSP on embedded Tomcat), `backend-nest/` (NestJS skeleton), `frontend/` (React + Vite).

Live targets: local dev only, `http://localhost:5173` (Vite) and `http://localhost:8085` (Java). MySQL was **not running**, so behaviour that needs the DB was inferred from code. HSTS and host/CDN headers can't be checked locally.

## Results

| # | Category | Status | Report | Plan |
|---|----------|--------|--------|------|
| 1 | SECRETS_EXPOSURE | HIGH | [report](reports/SECRETS_EXPOSURE_REPORT.md) | [plan](plans/SECRETS_EXPOSURE_PLAN.md) |
| 2 | DATABASE_ACCESS | N/A | [report](reports/DATABASE_ACCESS_REPORT.md) | [plan](plans/DATABASE_ACCESS_PLAN.md) |
| 3 | AUTH_MIDDLEWARE | CRITICAL | [report](reports/AUTH_MIDDLEWARE_REPORT.md) | [plan](plans/AUTH_MIDDLEWARE_PLAN.md) |
| 4 | ACCESS_CONTROL | LOW | [report](reports/ACCESS_CONTROL_REPORT.md) | [plan](plans/ACCESS_CONTROL_PLAN.md) |
| 5 | FRONTEND_SECRETS | PASS | [report](reports/FRONTEND_SECRETS_REPORT.md) | [plan](plans/FRONTEND_SECRETS_PLAN.md) |
| 6 | SSRF | PASS | [report](reports/SSRF_REPORT.md) | [plan](plans/SSRF_PLAN.md) |
| 7 | CSRF | MEDIUM | [report](reports/CSRF_REPORT.md) | [plan](plans/CSRF_PLAN.md) |
| 8 | SECURITY_HEADERS | MEDIUM | [report](reports/SECURITY_HEADERS_REPORT.md) | [plan](plans/SECURITY_HEADERS_PLAN.md) |
| 9 | CORS | PASS | [report](reports/CORS_REPORT.md) | [plan](plans/CORS_PLAN.md) |
| 10 | RATE_LIMITING | LOW | [report](reports/RATE_LIMITING_REPORT.md) | [plan](plans/RATE_LIMITING_PLAN.md) |
| 11 | SQL_INJECTION | PASS | [report](reports/SQL_INJECTION_REPORT.md) | [plan](plans/SQL_INJECTION_PLAN.md) |
| 12 | XSS | MEDIUM | [report](reports/XSS_REPORT.md) | [plan](plans/XSS_PLAN.md) |
| 13 | PAYMENT_WEBHOOKS | N/A | [report](reports/PAYMENT_WEBHOOKS_REPORT.md) | [plan](plans/PAYMENT_WEBHOOKS_PLAN.md) |
| 14 | FILE_UPLOADS | LOW | [report](reports/FILE_UPLOADS_REPORT.md) | [plan](plans/FILE_UPLOADS_PLAN.md) |
| 15 | ERROR_HANDLING | LOW | [report](reports/ERROR_HANDLING_REPORT.md) | [plan](plans/ERROR_HANDLING_PLAN.md) |
| 16 | PASSWORD_HASHING | CRITICAL | [report](reports/PASSWORD_HASHING_REPORT.md) | [plan](plans/PASSWORD_HASHING_PLAN.md) |
| 17 | DEPENDENCIES | LOW | [report](reports/DEPENDENCIES_REPORT.md) | [plan](plans/DEPENDENCIES_PLAN.md) |

Status is the state found **before** fixes. Each report's "Verification result" says what changed.

## Critical issues

1. **Auth bypass with a `;.json` path parameter** (AUTH_MIDDLEWARE): anonymous access to every `/api/*` route, including rewriting role permissions. Confirmed live. **Fixed** in `SessionAuthFilter`, with a regression test, verified on a patched server.
2. **Backdoor admin login** with the public default password `Tms@2026` (`admin` or `A25200212001@tms.vn`) through the in-memory `UserStore` fallback, even when the DB says the password is wrong. Confirmed live. **Fixed**: the fallback now runs only when the DB can't be reached.
3. **Every account reset to `Tms@2026` on each backend start** (`DatabaseMigration.resetPasswords`). **Fixed**: the reset now runs only when `RESET_PASSWORDS_ON_START=true` is in that machine's `backend/.env`.

## Other fixes applied

- SameSite=Lax on `JSESSIONID` (CSRF).
- `X-Content-Type-Options`, `X-Frame-Options` and `Referrer-Policy` on every backend response.
- Adding `;x` to the path no longer gets around the rate limit.
- User data HTML-escaped (`<c:out>`) in `user-import.jsp`, `user-roles.jsp` and `dashboard.jsp`.

## Live scan before and after (backend, `check.py`)

| | FAIL | WARN | PASS |
|---|---|---|---|
| Before (:8085, original code) | 4 | 3 | 5 |
| After (:8086, patched code) | 2 | 0 | 10 |

Remaining backend FAILs: CSP missing; Swagger UI public.

Frontend (:5173, Vite dev server, unchanged): 5 FAIL / 3 WARN / 9 PASS. The header FAILs come from the Vite dev server and have to be fixed in the production host's config. The Swagger FAIL is the backend behind the proxy.

All results are **local only** and say nothing yet about any deployment.

## Tools

- `npm audit`: production dependencies clean in all three Node packages. `backend-nest` dev dependencies have 1 high and 1 moderate (build-time only).
- `gitleaks`: not installed, so git history wasn't scanned (`winget install gitleaks`).
- `semgrep`: not installed (`pip install semgrep`).
- Maven dependency CVE scan: not run.

## Remaining manual verification

- Restart your own backend (:8085) so it loads the fixes.
- With MySQL up: a student calling `GET /api/admin/users` gets 403, and admin login with `Tms@2026` after a password change gets 401.
- If you want the old reset-on-start behaviour locally, add `RESET_PASSWORDS_ON_START=true` to your own `backend/.env`.
- Add CSP (report-only first) and HSTS on the real host, and set the cookie `Secure` under HTTPS.
- Import an Excel row whose name is `<img src=x onerror=alert(1)>`. The preview must show it as text.
- Run gitleaks on the history and rotate anything it finds.
