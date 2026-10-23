# RATE_LIMITING Fix Plan

## Changes

- `backend/src/main/java/com/ems/filter/AuthRateLimitFilter.java`: key from `getServletPath()+getPathInfo()`. **Done.**

## New files

- None.

## Verification goals

- [x] Path-parameter variants share one bucket (code)
- [x] 429 after the limit (existing behaviour)
- [ ] Correct client IP behind a proxy

## Manual verification (for the human)

- Send 31 logins to `/api/auth/login;N` with a different N each time. The 31st should get 429.
