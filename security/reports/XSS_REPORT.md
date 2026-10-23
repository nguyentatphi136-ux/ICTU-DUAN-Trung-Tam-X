# XSS Security Report

## Status: MEDIUM

## Findings

- React frontend: no `dangerouslySetInnerHTML` with data. `Icon.tsx:57` injects static SVG paths from a constant map, and `Markdown.tsx` renders without innerHTML.
- **JSP (FIXED):** JSP EL `${...}` is not HTML-escaped, and user-controlled values were printed raw:
  - `WEB-INF/views/admin/user-import.jsp`: names, emails, error reasons and file name from the uploaded Excel (the preview table), plus `errorMessage` and the hidden input `value="${uploadedFileName}"`. A crafted import file runs script in the admin's session.
  - `dashboard.jsp`: the user's own name and email (editable through `/api/profile`).
  - `WEB-INF/views/admin/user-roles.jsp`: user name and email, `errorMessage`.
  All of these are now wrapped in `<c:out>`.
- Still raw: `user-roles.jsp:136` puts `${u.name}` inside an `onsubmit="confirm('...')"` JS string. The names come from the hardcoded `UserStore`, so they aren't attacker-controlled today. `c:out` wouldn't be correct there, because the browser decodes HTML entities before the JS runs.

## What's at risk

Before the fix, a malicious spreadsheet shown in the admin import preview could act as the admin.

## What's already secure

React escapes by default, and the team avoided innerHTML.

## Recommendations

1. (Done) `<c:out>` for user data in the JSPs.
2. Move the confirm text into a `data-` attribute read by JS, or drop the name from the message.
3. Add CSP (see SECURITY_HEADERS).

## Verification result

Code fixed; the JSPs weren't rendered here (that needs a DB).
