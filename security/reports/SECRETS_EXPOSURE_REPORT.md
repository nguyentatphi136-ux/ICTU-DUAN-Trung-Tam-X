# SECRETS_EXPOSURE Security Report

## Status: HIGH

## Findings

- `backend/src/main/java/com/ems/dao/UserDAO.java:28` hardcodes `DEFAULT_PASSWORD = "Tms@2026"`. The repo `nguyentatphi136-ux/ICTU-DUAN-Trung-Tam-X` is **public** on GitHub, so this password is public.
- That password is used for: the in-memory fallback accounts (`service/UserStore.java`: `A25200212001@tms.vn`, plus aliases like `admin`, `admin@example.com`), the startup reset of every DB account (`config/DatabaseMigration.java:262`), new students created from leads (`dao/StudentDAO.java:83`), and the activation email body (`controller/LeadApiServlet.java:110`).
- `backend-nest/src/auth/demo-accounts.ts:7` falls back to `DEMO_PASSWORD = '123456'` when the env var is unset. It's disabled when `NODE_ENV=production`, which the Dockerfile sets.
- `backend/src/main/resources/database.properties` is tracked: `DB_USER=root`, `DB_PASSWORD=` (empty). That's a local default, not a leaked secret, but "root with no password" is the shipped config.
- `.env` and `frontend/.env.local` (`*.local`) are gitignored and untracked. All `.env.example` files hold placeholders only.
- The frontend only uses `VITE_API_URL` (a URL, not a secret).
- Live: `/.env`, `/.git/config`, dumps, keys and logs return 404 on both :5173 and :8085, and directory listing is off.
- `gitleaks` is not installed, so git history was **not** scanned.

## What's at risk

Anyone who reads the public repo knows the password that every account gets reset to on each backend restart (see PASSWORD_HASHING) and the password of the fallback admin.

## What's already secure

`.env` handling is correct. No API keys, JWT secrets or SMTP passwords are committed in current files. No sensitive files are served.

## Recommendations

1. Stop using a shared, committed default password (see PASSWORD_HASHING and AUTH_MIDDLEWARE).
2. Run `gitleaks detect --source . --verbose` on the history (install: `winget install gitleaks`).
3. Use a non-root DB user in any deployment.

## Verification result

Partly. The `.env` checks pass. The default-password items stay open until the team decides.
