# Key Flows

## Public page load

Browser requests the frontend through Cloudflare; the React app requests only public REST data needed for the page. Public assets are delivered through configured storage/CDN paths. Public data must not reveal admin or private result information.

## Principal login and session

```mermaid
sequenceDiagram
  participant B as Browser SPA
  participant A as Spring Boot API
  participant P as PostgreSQL
  B->>A: GET /api/v1/admin/auth/csrf
  A-->>B: XSRF-TOKEN cookie and token/header name
  B->>A: POST /login with CSRF header and credentials
  A->>P: Verify active principal and BCrypt hash
  A->>P: Save LOGIN_SUCCESS audit event
  A->>P: Persist Spring Session JDBC record
  A-->>B: HttpOnly session cookie and safe account view
  B->>A: Protected request with session cookie and CSRF on mutation
  A->>P: Load server-side session
```

The server rotates the session ID at login and password change. Logout invalidates the JDBC session. Login failure is generic and rate-limited; submitted credentials are not logged. The frontend maps error codes to Gujarati text.

## Content and files

For study material, notices, gallery, downloads, and timetable files, the principal validates metadata in the admin UI. The backend validates authorization, type, size, and storage handling; it stores an object and its metadata/key transactionally as far as possible. Public access is permitted only after the content's intended publication state. If database persistence fails after an object write, cleanup is attempted and the failure is logged for follow-up.

## Assessment and results

```mermaid
flowchart LR
  A[Create generic assessment] --> B[Obtain owner-provided Excel contract]
  B --> C[Upload workbook]
  C --> D[Validate and preview]
  D --> E[Confirm transactional import]
  E --> F[Draft results]
  F --> G[Explicit principal publication]
  G --> H[Individual public lookup]
```

The workbook contract is not defined. Do not infer sheet names, columns, marks representation, PIN behavior, or calculations. A published lookup requests only an individual result using the future approved identifiers and must receive `Cache-Control: no-store`.

## School configuration and academic-year transition

The principal changes school-owned settings through a protected admin workflow; public pages read configured data rather than source constants. Academic-year transition is an explicit administrative process: create/select the next year, keep historical records linked to their year, and archive rather than silently rewrite historical data. Exact rollover business rules remain to be specified.
