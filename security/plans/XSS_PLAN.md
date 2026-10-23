# XSS Fix Plan

## Changes

- `backend/src/main/webapp/WEB-INF/views/admin/user-import.jsp`: 11 outputs escaped. **Done.**
- `backend/src/main/webapp/dashboard.jsp`: 3 outputs escaped. **Done.**
- `backend/src/main/webapp/WEB-INF/views/admin/user-roles.jsp`: 4 outputs escaped. **Done.**

## New files

- None.

## Verification goals

- [x] No raw `${}` of user data in HTML text or attributes in these JSPs
- [ ] JS-string case at `user-roles.jsp:136` reworked

## Manual verification (for the human)

- Import an Excel row whose full name is `<img src=x onerror=alert(1)>` through `/admin/user-import`. The preview should show the text, not run it.
