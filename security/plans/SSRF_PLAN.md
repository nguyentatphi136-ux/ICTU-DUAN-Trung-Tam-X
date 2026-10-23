# SSRF Fix Plan

## Changes

- `controller/LeadApiServlet.java`: replace `siteOrigin(req)` with the `APP_BASE_URL` setting.

## New files

- None.

## Verification goals

- [ ] Activation email link host comes from config

## Manual verification (for the human)

- None.
