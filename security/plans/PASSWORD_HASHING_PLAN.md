# PASSWORD_HASHING Fix Plan

## Changes

- `backend/src/main/java/com/ems/config/DatabaseMigration.java`: wrap `resetPasswords(conn)` in `if ("true".equals(DBConnection.setting("RESET_PASSWORDS_ON_START", "false")))`, and add the flag to `backend/.env.example`.
- `dao/StudentDAO.java`, `dao/AdminDAO.java`, `controller/LeadApiServlet.java`: random per-user initial password.

## New files

- None.

## Verification goals

- [x] BCrypt only
- [x] A restart doesn't change any `password_hash` unless the opt-in flag is set
- [ ] No shared initial password

## Manual verification (for the human)

- Decide as a team: does local testing depend on the startup reset? If so, keep it behind the flag in your local `.env`.
