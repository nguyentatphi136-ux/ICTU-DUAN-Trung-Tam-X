# SSRF Security Report

## Status: PASS

## Findings

The server never fetches a user-supplied URL: no `HttpURLConnection`, `HttpClient` or `URL.openStream` on request data in the backend, and no outbound HTTP in Nest.
Related: `LeadApiServlet.siteOrigin()` builds the login link in the activation email from the request `Origin` header (only checked to look like `https?://host`). A user with `LEAD_MANAGE` can make the system email a student a link to any host. That's phishing from a trusted sender, not SSRF.

## What's at risk

Email link injection by a logged-in staff member.

## What's already secure

No outbound requests.

## Recommendations

Build email links from the configured `APP_BASE_URL` (already used in `ForgotPasswordServlet`) instead of `Origin`.

## Verification result

Not fixed (low).
