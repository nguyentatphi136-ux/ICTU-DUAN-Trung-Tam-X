# SECURITY_HEADERS Security Report

## Status: MEDIUM

## Findings

Before (live, :8085 and :5173): no CSP, no X-Frame-Options, no X-Content-Type-Options, no Referrer-Policy. The repo has no hosting config (`vercel.json`, `netlify.toml`, nginx), and the production host is unknown.

**Fixed (backend):** `SessionAuthFilter` (mapped to `/*`) now sends `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` and `Referrer-Policy: strict-origin-when-cross-origin` on every response, static files included. Live re-scan on :8086: all three PASS.

Still missing: **CSP** (backend and frontend) and **HSTS** (skipped locally; the HTTPS host must set it).

## What's at risk

Clickjacking of admin pages (now blocked on the backend). Without CSP, any XSS runs unrestricted.

## What's already secure

CORS is closed. Error pages are generic.

## Recommendations

1. Add CSP as `Content-Security-Policy-Report-Only` first. The JSP pages load Bootstrap from `cdn.jsdelivr.net`, so start with `default-src 'self'; script-src 'self' https://cdn.jsdelivr.net; style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; font-src 'self' https://cdn.jsdelivr.net; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'`. Check whether the JSPs use inline `<script>` before enforcing it.
2. Set the same headers plus HSTS (`max-age=31536000; includeSubDomains`) on whatever serves the built frontend.

## Verification result

Partly.
