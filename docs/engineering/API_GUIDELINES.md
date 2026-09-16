# API Guidelines

Use versioned REST paths: `/api/v1/public` for unauthenticated resources and `/api/v1/admin` for authenticated principal operations. Do not create a broad public data endpoint simply for frontend convenience.

## Implemented admin endpoints

| Method and path | Access | Purpose |
| --- | --- | --- |
| `GET /api/v1/admin/auth/csrf` | Public | Supplies the SPA CSRF token and header name. |
| `POST /api/v1/admin/auth/login` | Public + CSRF | Authenticates an active principal and establishes a JDBC-backed session. |
| `POST /api/v1/admin/auth/logout` | Public + CSRF | Invalidates an existing session; repeated logout is safe. |
| `GET /api/v1/admin/auth/me` | `PRINCIPAL` | Returns email, role, and `mustChangePassword`; never credentials or session data. |
| `PUT /api/v1/admin/auth/password` | `PRINCIPAL` + CSRF | Verifies current password and sets a new 12–72 character password. |
| `GET /api/v1/admin/dashboard/summary` | `PRINCIPAL` | Returns operational stats (students, standards, subjects, materials, notices, gallery). |
| `GET /api/v1/admin/audit-logs` | `PRINCIPAL` | Returns paginated audit logs for principal security overview. |
| `GET /api/v1/admin/academic/students` | `PRINCIPAL` | Lists students scoped to standard and academic year. |
| `POST /api/v1/admin/academic/students` | `PRINCIPAL` + CSRF | Enrolls new student under standard and academic year. |
| `PUT /api/v1/admin/academic/students/{id}` | `PRINCIPAL` + CSRF | Updates student name, roll number, or PIN hash. |
| `GET /api/v1/admin/materials` | `PRINCIPAL` | Lists every school-scoped material, including drafts and archived records. |
| `GET /api/v1/admin/notices` | `PRINCIPAL` | Lists every school-scoped notice, including drafts and archived records. |
| `GET /api/v1/admin/gallery/albums` | `PRINCIPAL` | Lists every school-scoped gallery album, including drafts and archived records. |
| `GET /api/v1/admin/gallery/albums/{id}/images` | `PRINCIPAL` | Lists images for a school-scoped album, including drafts. |
| `GET /api/v1/admin/downloads` | `PRINCIPAL` | Lists every school-scoped download, including drafts and archived records. |

Use DTOs and Jakarta Validation. Never return JPA entities. Authentication failures remain generic, and all frontend-visible text is represented by stable codes for Gujarati mapping.

- Use Problem Detail-style JSON with `code` and `requestId`; never disclose sensitive internals.
- Use 200/204 for success, 400 for validation or password-change rejection, 401 for absent/invalid authentication, 403 for forbidden/CSRF failure, 404 for an absent public resource, 409 for conflict, and 429 for rate limiting.
- Admin mutations require a valid CSRF token. The frontend obtains it from the CSRF endpoint and sends it in the returned header name.
- Credentialed CORS permits only configured origins. Authenticated endpoints never return wildcard origins.
- Paginated list endpoints define page size limits and response metadata. Filtering and sorting use explicit whitelists, never raw field names.
- Individual result responses will use `Cache-Control: no-store`; failed lookups must be generic enough to resist enumeration.

## Current endpoint inventory

Authentication, school/profile, academic configuration, student management, dashboard summary, audit logs, generic assessment configuration, and ImageKit-backed materials, notices, gallery, and downloads have versioned DTO endpoints. Admin mutations require the principal session and CSRF. Public school/profile, current academic configuration, assessment-type, and public content routes expose published data only.

## ImageKit delivery

The browser sends files only to authenticated backend multipart endpoints. The backend validates the upload, writes it to ImageKit through the storage abstraction, and stores only ImageKit bucket/key and file metadata in PostgreSQL. Published DTOs contain a URL derived from the configured ImageKit public endpoint; gallery-image DTOs also include an ImageKit transformation URL for responsive thumbnails. Draft and archived content is absent from public routes; the authenticated admin gallery route may return image URLs so the principal can preview draft uploads. There is no browser-direct upload-auth endpoint or R2 integration in the current implementation.

Result import, result publication, available-result choices, and individual result lookup are blocked by the documented privacy-safe student-identity decision. Other exam formats and timetables have no endpoint contract until source material is supplied.

All `/api/v1/admin/**` responses use `Cache-Control: no-store`.
