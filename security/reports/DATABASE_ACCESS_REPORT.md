# DATABASE_ACCESS Security Report

## Status: N/A

## Findings

No Supabase/Firebase. MySQL is reached only from the backend (JDBC in `config/DBConnection.java`, `mysql2` in backend-nest). No client-side DB access, so RLS doesn't apply.
Note: the shipped config connects as `root` with an empty password (`database.properties`).

## What's at risk

Nothing from browsers. A deployment running as `root` gives any SQL bug server-wide DB access.

## What's already secure

No direct DB access from the frontend.

## Recommendations

Use a dedicated MySQL user limited to `ems_database` in any shared or deployed environment.

## Verification result

N/A
