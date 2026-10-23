# SECRETS_EXPOSURE Fix Plan

## Changes

- `dao/UserDAO.java`: read the initial password from config, or generate a random one per account.
- `config/DatabaseMigration.java`: see the PASSWORD_HASHING plan.

## New files

- None.

## Verification goals

- [x] `git ls-files .env` returns nothing
- [ ] No password literal shared across all accounts in source
- [ ] gitleaks reports nothing in history

## Manual verification (for the human)

- Install gitleaks and scan the full history.
- If a real SMTP/DB password was ever committed, rotate it.
