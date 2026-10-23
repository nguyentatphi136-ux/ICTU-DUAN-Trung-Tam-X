# FILE_UPLOADS Security Report

## Status: LOW

## Findings

- The only upload is the Excel/CSV user import (`vn/edu/ictu/ems/controller/UserImportServlet.java`), gated by `USER_CREATE`. The server enforces `@MultipartConfig(maxFileSize = 10 MB)`. Files are parsed in memory (Apache POI 5.2.5 / CSV) and never stored or served back, so renaming and separate storage don't apply.
- POI 5.x disables XXE and applies `ZipSecureFile` zip-bomb limits by default.
- The avatar (S2-03) is frontend-only (cropped in the browser). There's no server endpoint, and the API never writes `avatar_url`.

## What's at risk

A crafted 10 MB xlsx can still use noticeable CPU and RAM while parsing (logged-in admins only).

## What's already secure

Size limit, permission gate, and uploaded bytes are never stored.

## Recommendations

When the avatar upload endpoint is built: check magic bytes (JPEG/PNG), use random file names, limit size, and serve from a separate path with nosniff.

## Verification result

N/A
