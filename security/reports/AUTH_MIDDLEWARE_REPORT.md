# AUTH_MIDDLEWARE Security Report

## Status: CRITICAL

## Findings

**A. Auth bypass with a URL path parameter (CRITICAL, confirmed live, FIXED).**
`filter/SessionAuthFilter.java` decided "public static file" from `req.getRequestURI()`, which keeps `;...` path parameters. Tomcat strips those parameters when it picks the servlet. So `/api/admin/users;.json` skipped the login check (the URI ends in `.json`) and still reached `AdminApiServlet`.

Live evidence on :8085, original code:
```
GET /api/admin/users          -> 401 AUTH_UNAUTHORIZED
GET /api/admin/users;.json    -> 503 (handler ran, DB down)
GET /api/admin/roles;x.js     -> 503 (handler ran)
GET /api/students;.json       -> 500 DATABASE_ERROR (handler ran)
```
With the DB up, an anonymous user could list every user (`GET /api/admin/users;.json`) and **rewrite the role-permission matrix** (`PUT /api/admin/roles/STUDENT/permissions;.json`), because `AdminApiServlet` does no auth of its own on that route. The same applies to training programs, subjects, rooms, students and classes.

**B. Backdoor login through the in-memory fallback (CRITICAL, confirmed live, FIXED).**
`controller/LoginApiServlet.java` called `UserStore.authenticate()` whenever the DB login returned `null`. That includes a **wrong password**, not just "DB down". `UserStore` has a hardcoded admin (id 1) with password `Tms@2026` and the aliases `admin` and `admin@example.com`. Live: `POST /api/auth/login {"email":"admin","password":"Tms@2026"}` returned 200, role admin, `mustChangePassword:false`. Permissions are then looked up by id in the DB, so on a real DB this is full admin, even after the real admin has changed their password.

**C. Route inventory (from `@WebServlet` and `PermissionPolicy`).**

| Route | Protection |
|---|---|
| `/api/auth/login`, `/forgot-password`, `/reset-password`, `/api/health`, `/api/public/*` | public by design, rate limited |
| `/api-docs/*`, `/api/docs/*` | public (see ERROR_HANDLING) |
| `/api/admin/*` | SessionAuthFilter + per-route permission from `PermissionPolicy`; unknown routes are denied by default (403) |
| `/api/leads/*` | `LEAD_MANAGE` / `LEAD_ASSIGN`; purge also checks ADMIN |
| `/api/training-programs/*`, `/api/subjects/*` | `PROGRAM_MANAGE` |
| `/api/classes`, `/api/students`, `/api/rooms` | `CLASS_MANAGE`; `/api/classes/mine` for any logged-in user |
| `/api/profile`, `/api/me/menu`, logout, change-password | any logged-in user |
| `/grade/*`, `/tuition/*` (JSP) | role checks in `AuthorizationFilter` |
| `/admin/users/roles`, `/admin/user-import` (JSP) | login + ADMIN / `USER_CREATE` check inside the servlet |

**D. Other notes.**
- `AuthRateLimitFilter` also keyed on the raw URI, so adding `;x` gave a fresh bucket (FIXED, see RATE_LIMITING).
- `AdminApiServlet.actorId()` returns `0` instead of rejecting when the attribute is missing. That's harmless now that the filter always sets it on `/api/`, but it's fragile.
- The JSP `UserRoleServlet` edits only the in-memory `UserStore`, not the DB.

## What's at risk

Before the fix, an anonymous attacker could read every user record and give any role every permission: full takeover. Separately, anyone could log in as admin with the public default password.

## What's already secure

Default-deny on `/api/*` (an unknown route gets 403) is a good design. The session id is rotated at login (`changeSessionId`). Logout blacklists the session. The forced change of a default password is enforced in the filter.

## Recommendations

1. (Done) Use `getServletPath()+getPathInfo()` for every path decision, and never treat `/api/*` as a static file.
2. (Done) Use the `UserStore` fallback only when the DB threw `SQLException`.
3. Consider deleting the `UserStore` fallback before any shared deployment.
4. Make `AdminApiServlet.actorId()` throw 401, like `JsonApiServlet.actorId()`.

## Verification result

Yes. A and B verified on the patched instance.
