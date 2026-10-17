# AUTH_MIDDLEWARE Fix Plan

## Changes

- `backend/src/main/java/com/ems/filter/SessionAuthFilter.java`: normalized path; `/api/` excluded from the static-suffix rule. **Done.**
- `backend/src/main/java/com/ems/controller/LoginApiServlet.java`: fallback only when `databaseDown`. **Done.**

## New files

- `backend/src/test/java/com/ems/test/SessionAuthFilterBypassTest.java` (regression test, 5 cases)

## Verification goals

- [x] Anonymous `GET /api/admin/users;.json`, `;x.js`, `;.css`, `;.html` return 401 (verified on the patched server, :8086)
- [x] `SessionAuthFilterBypassTest` passes; full suite 96/96
- [x] A DB answer of "wrong password" no longer falls through to `UserStore` (code path; the DB-down fallback still returns 200 by design)
- [ ] Non-admin on admin routes gets 403 (existing `PermissionPolicy`; needs a DB to check live)

## Manual verification (for the human)

- With MySQL running: log in as a student and call `GET /api/admin/users`. Expect 403.
- With MySQL running: change the admin password, then `POST /api/auth/login {"email":"admin","password":"Tms@2026"}`. Expect 401.
- Restart your own :8085 server so it picks up the fix.
