# ACCESS_CONTROL Security Report

## Status: LOW

## Findings

- Leads: every by-id operation passes `actor` + `seeAll(actor)` (`LeadApiServlet.java`), and the service filters to the owner unless the user has `LEAD_ASSIGN`. Covered by `S2ApiServletTest.leads`.
- Profile: `/api/profile` always uses the session user id and ignores any id/email/role in the body.
- Admin user routes are permission-gated. Purge also requires ADMIN. Self-demotion is guarded by `PermissionPolicy.mayReplaceRoles`.
- `GET /api/leads/check-phone` returns matching leads whoever owns them (duplicate warning, by design). Small cross-owner leak: the name and owner of a lead with that phone.
- Classes, students and rooms are scoped by role (`CLASS_MANAGE`), not by owner. That matches the spec (the training manager sees everything).

## What's at risk

A counselor can learn that another counselor has a lead with a given phone number.

## What's already secure

Ownership is checked separately from authentication, on GET, PUT and DELETE for leads.

## Recommendations

Optional: return only id and owner name from `check-phone` to users without `LEAD_ASSIGN`.

## Verification result

No code change.
