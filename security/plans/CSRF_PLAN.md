# CSRF Fix Plan

## Changes

- `backend/src/main/java/com/ems/bootstrap/TomcatBootstrap.java`: SameSite=Lax cookie processor. **Done.**

## New files

- None.

## Verification goals

- [x] JSESSIONID has HttpOnly + SameSite=Lax (verified live on :8086, scanner PASS)
- [ ] Secure flag on the HTTPS deployment
- [ ] A cross-site form POST to `/api/admin/...` fails (manual)

## Manual verification (for the human)

- From another origin, submit a form POST to `/admin/users/roles` while logged in. It should arrive without a session.
