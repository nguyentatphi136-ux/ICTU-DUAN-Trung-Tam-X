# RATE_LIMITING Security Report

## Status: LOW

## Findings

- `filter/AuthRateLimitFilter.java`: 30 requests per 15 min per IP+path for login and reset; 5 for forgot-password and the public consultation form. Returns 429 with `Retry-After`. It uses `getRemoteAddr()`, not `X-Forwarded-For`, so a spoofed header can't get around it.
- Per-account lockout exists (`security/LoginAttemptPolicy.java`, `failed_login_attempts`, `locked_until`).
- **Bug (FIXED):** the key used the raw URI, so `/api/auth/login;1`, `;2` and so on each got a fresh bucket. It now uses the normalized servlet path.
- Behind a reverse proxy, `getRemoteAddr()` is the proxy's IP and all users share one bucket. Configure Tomcat's `RemoteIpValve` with the trusted proxy list when deploying.
- `ForgotPasswordServlet.RATE_LIMIT_CACHE` is never pruned (slow memory growth).

## What's at risk

Before the fix, the per-IP login limit could be bypassed (account lockout still applied).

## What's already secure

Every auth endpoint is limited, returns 429, and isn't fooled by a spoofed `X-Forwarded-For`.

## Recommendations

1. (Done) Normalized key.
2. `RemoteIpValve` behind a proxy.
3. Prune `RATE_LIMIT_CACHE`.

## Verification result

Yes (code change; tests pass).
