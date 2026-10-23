# SECURITY_HEADERS Fix Plan

## Changes

- `backend/src/main/java/com/ems/filter/SessionAuthFilter.java`: three headers. **Done.**
- Frontend hosting config: **to do once a host is chosen.**

## New files

- None.

## Verification goals

- [x] nosniff / X-Frame-Options / Referrer-Policy on backend pages and static files (live PASS)
- [ ] CSP declared (report-only first)
- [ ] HSTS on the HTTPS deployment
- [ ] Same headers on the built frontend

## Manual verification (for the human)

- After adding a report-only CSP, click through the JSP pages and watch the console for violations.
