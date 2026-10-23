# CSRF Security Report

## Status: MEDIUM

## Findings

- Auth is a `JSESSIONID` cookie. Before the fix it was `HttpOnly` with **no SameSite** (live scan WARN) and `<secure>false</secure>` in `web.xml`.
- There are no CSRF tokens. JSON endpoints read the body without checking `Content-Type` (`JsonApiServlet.body`), so a cross-site `fetch(..., {mode:'no-cors', credentials:'include'})` with a `text/plain` JSON body is accepted. The JSP forms (`/admin/users/roles`, `/admin/user-import`, change-password) are plain POST forms.
- Chrome's Lax-by-default limited this, but Firefox and Safari don't apply it.
- `GET /logout` changes state (low).

**Fixed:** `TomcatBootstrap` now sets `Rfc6265CookieProcessor.setSameSiteCookies("Lax")`. Live on the patched server: `Set-Cookie: JSESSIONID=...; Path=/; HttpOnly; SameSite=Lax`.

## What's at risk

A malicious page opened by a logged-in admin could assign roles, change permissions or lock accounts.

## What's already secure

HttpOnly is set and the session id rotates at login.

## Recommendations

1. (Done) SameSite=Lax.
2. Set `Secure` on the cookie when served over HTTPS (`<secure>true</secure>` in the deployed `web.xml`).
3. Optional: reject bodies that aren't `application/json` on JSON APIs.
4. If deployed as a WAR to a standalone Tomcat, add the same `CookieProcessor` in `META-INF/context.xml`. `TomcatBootstrap` only covers the embedded launcher.

## Verification result

Yes (SameSite).
