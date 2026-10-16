# PASSWORD_HASHING Security Report

## Status: CRITICAL

## Findings

- The hashing itself is good: BCrypt cost 10 (`jbcrypt 0.4`) everywhere a password is stored (`AdminDAO`, `ResetPasswordServlet`, `StudentDAO`, `UserDAO`). Reset tokens are random UUIDs, stored as SHA-256, expire after 30 minutes and are single-use inside a transaction. That's fine for tokens.
- **CRITICAL, FIXED (opt-in flag):** `config/DatabaseMigration.runMigration()`, called on every startup by `AppStartupListener`, runs `resetPasswords(conn)`:
  ```java
  UPDATE users SET password_hash = <bcrypt("Tms@2026")>, failed_login_attempts = 0, locked_until = NULL
  ```
  Every account, admins included, gets the public password `Tms@2026` and is unlocked on each restart. The doc comment says "chạy local", but nothing checks that it's running locally. On any shared or deployed DB, an attacker can log in as anyone after a restart using the password from the public repo, and the "must change password" screen then lets them set the victim's new password.
- New students created from leads get the same `Tms@2026`, and it's sent in the activation email.

## What's at risk

Every account can be taken over after any backend restart on a non-local DB.

## What's already secure

BCrypt everywhere, no MD5/SHA for passwords, a sound reset-token design, and a forced change of the default password.

## Recommendations

1. Put `resetPasswords` behind an explicit opt-in env flag (e.g. `RESET_PASSWORDS_ON_START=true` only in local `.env`), or delete it.
2. Give each new account a random initial password (or an activation link) instead of a shared default.

## Verification result

Fixed. `resetPasswords` now runs only when `RESET_PASSWORDS_ON_START=true` is set (backend/.env). Default is off, so a restart keeps changed passwords. The shared initial password for new accounts is still open.
