# DATABASE_ACCESS Fix Plan

## Changes

- Deployment config only: create an `ems_app` user with `SELECT, INSERT, UPDATE, DELETE ON ems_database.*`.

## New files

- None.

## Verification goals

- [ ] Backend connects as a non-root user outside local dev

## Manual verification (for the human)

- Create the DB user on the server.
