# Key Flows — Rorfost School Platform

## 1. Public Page Load and API Routing

### Current State (As-Is)
Browser visits `portal.dhadhana-school.workers.dev`; the single Cloudflare Worker serves the static React SPA and proxies `/api/*` directly to Render without tenant authentication tokens. The backend loads the first active school row.

### Target State (Multi-Tenant Plan)
```mermaid
sequenceDiagram
  participant B as Browser (school-a.workers.dev)
  participant W as Cloudflare Worker (School A)
  participant R as Render Backend
  participant D as Shared Neon DB
  B->>W: GET /api/v1/public/school
  W->>W: Strip any client-supplied tenant headers
  W->>W: Inject X-Portal-Gateway-Token (Secret A)
  W->>R: HTTPS Forward with secret token
  R->>D: Hash token (SHA-256) & lookup active school_id
  D-->>R: Active School A metadata
  R->>R: Bind immutable TenantContext (School A)
  R->>D: Query public data WHERE school_id = School A
  D-->>R: School A data
  R-->>W: JSON response
  W-->>B: JSON response
```

## 2. Principal Login and Session Lifecycle

```mermaid
sequenceDiagram
  participant B as Browser SPA
  participant W as Cloudflare Worker
  participant A as Spring Boot API
  participant P as PostgreSQL
  B->>W: GET /api/v1/admin/auth/csrf
  W->>A: Proxy with X-Portal-Gateway-Token
  A-->>B: XSRF-TOKEN cookie & header name
  B->>W: POST /login with credentials (email, password)
  W->>A: Proxy with X-Portal-Gateway-Token
  A->>A: Verify TenantContext from Gateway Token
  A->>P: Verify active principal WHERE school_id = TenantContext.schoolId AND email = :email
  A->>P: Verify BCrypt password hash
  A->>P: Save LOGIN_SUCCESS audit event (scoped to school_id)
  A->>P: Persist Spring Session JDBC record with schoolId attribute
  A-->>B: HttpOnly session cookie and safe account view
  B->>W: Admin request with session cookie
  W->>A: Proxy with X-Portal-Gateway-Token
  A->>A: Enforce PrincipalSession.schoolId == TenantContext.schoolId
  A->>P: Perform administrative query/mutation (school_id scoped)
```

The server rotates the session ID at login and password change. Logout invalidates the JDBC session. In the target multi-tenant model, authenticating at School A's portal cannot grant access to School B, even if the principal uses the same email address across schools.

## 3. Content and File Management

For study materials, notices, gallery albums, and downloads:
1. The principal uploads files via the admin UI.
2. The backend validates file format (signature checks for PDF, JPEG, PNG, WebP) and size limits.
3. Target architecture stores assets under a tenant-specific prefix in ImageKit: `schools/<school-uuid>/[materials|gallery|branding|downloads]/...`.
4. The database stores the object key, provider file ID, and metadata within a service transaction scoped to `school_id`.
5. Compensating cleanup is attempted if database persistence fails after an ImageKit upload.

## 4. Assessment and Results Workflow

```mermaid
flowchart LR
  A[Create generic assessment] --> B[Obtain approved Excel template]
  B --> C[Upload workbook via admin]
  C --> D[Validate rows & preview]
  D --> E[Confirm transactional import]
  E --> F[Draft results created]
  F --> G[Explicit principal publication]
  G --> H[Individual public lookup with no-store]
```

- Excel imports replace results only within the active `school_id` and selected result type (`ANNUAL` or `EKAM_KASOTI`).
- Student rows are matched using school-controlled roll numbers and normalized names; government identifiers (such as Aadhaar) are never read or stored.
- Public lookup queries strictly enforce `school_id = TenantContext.schoolId` and return `Cache-Control: no-store`.

## 5. School Configuration & Academic Transition

The principal manages school settings, branding, and academic configuration (academic years, standards, subjects) through protected admin pages. All mutations are strictly isolated to `school_id`. Academic year transition is an explicit administrative action: creating a new year, marking it current, and archiving the previous year without mutating historical marks.
