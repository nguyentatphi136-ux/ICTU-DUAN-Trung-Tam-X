# ERROR_HANDLING Fix Plan

## Changes

- `filter/SessionAuthFilter.java`: remove `/api-docs` and `/api/docs` from the public list. Team decision, since frontend devs may rely on it.

## New files

- None.

## Verification goals

- [x] No stack traces in responses (live PASS)
- [ ] API docs return 401 or 404 in production

## Manual verification (for the human)

- Decide whether the docs stay public in dev.
