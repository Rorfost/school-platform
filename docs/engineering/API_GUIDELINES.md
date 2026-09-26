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
| `POST /api/v1/admin/school/logo` | `PRINCIPAL` + CSRF | Validates and stores a JPEG, PNG, or WebP school logo through ImageKit. |
| `DELETE /api/v1/admin/school/logo` | `PRINCIPAL` + CSRF | Removes the configured school logo and attempts ImageKit cleanup. |
| `GET /api/v1/admin/dashboard/summary` | `PRINCIPAL` | Returns operational stats (students, standards, subjects, materials, notices, gallery). |
| `GET /api/v1/admin/audit-logs` | `PRINCIPAL` | Returns paginated audit logs for principal security overview. |
| `GET /api/v1/admin/academic/students` | `PRINCIPAL` | Lists students scoped to standard and academic year. |
| `POST /api/v1/admin/academic/students` | `PRINCIPAL` + CSRF | Enrolls new student under standard and academic year. |
| `PUT /api/v1/admin/academic/students/{id}` | `PRINCIPAL` + CSRF | Updates student name, roll number, or PIN hash. |
| `GET /api/v1/admin/materials` | `PRINCIPAL` | Lists every school-scoped material, including drafts and archived records. |
| `GET /api/v1/admin/notices` | `PRINCIPAL` | Lists every school-scoped notice, including drafts and archived records. |
| `GET /api/v1/admin/gallery/albums` | `PRINCIPAL` | Lists every school-scoped gallery album, including drafts and archived records. |
| `GET /api/v1/admin/gallery/albums/{id}/images` | `PRINCIPAL` | Lists images for a school-scoped album, including drafts. |
| `POST /api/v1/admin/gallery/albums` | `PRINCIPAL` + CSRF | Creates a draft gallery album. |
| `PUT /api/v1/admin/gallery/albums/{id}` | `PRINCIPAL` + CSRF | Updates a non-archived album title and description. |
| `POST /api/v1/admin/gallery/albums/{id}/publish` | `PRINCIPAL` + CSRF | Publishes the album and all non-archived images in it. |
| `POST /api/v1/admin/gallery/albums/{id}/unpublish` | `PRINCIPAL` + CSRF | Returns the album and its published images to draft visibility. |
| `DELETE /api/v1/admin/gallery/albums/{id}` | `PRINCIPAL` + CSRF | Deletes album metadata and its ImageKit objects. |
| `POST /api/v1/admin/gallery/albums/{id}/images` | `PRINCIPAL` + CSRF | Uploads one image; the server assigns its next display order. |
| `POST /api/v1/admin/gallery/albums/{id}/images/batch` | `PRINCIPAL` + CSRF | Uploads ordered files using matching `altTexts` and optional `captions` multipart fields. |
| `PUT /api/v1/admin/gallery/albums/{id}/cover` | `PRINCIPAL` + CSRF | Selects an image from that album as its cover. |
| `PUT /api/v1/admin/gallery/albums/{albumId}/images/{imageId}` | `PRINCIPAL` + CSRF | Updates image alt text and caption. |
| `PATCH /api/v1/admin/gallery/albums/{id}/images/reorder` | `PRINCIPAL` + CSRF | Replaces the complete image order with a validated `imageIds` list. |
| `DELETE /api/v1/admin/gallery/albums/{albumId}/images/{imageId}` | `PRINCIPAL` + CSRF | Deletes the ImageKit object and image metadata, then normalizes order. |
| `GET /api/v1/admin/downloads` | `PRINCIPAL` | Lists every school-scoped download, including drafts and archived records. |
| `POST /api/v1/admin/exam-results/upload` | `PRINCIPAL` + CSRF | Imports the approved multi-subject workbook for either result type. The multipart `resultType` (`ANNUAL` or `EKAM_KASOTI`) selects the isolated result set to replace. |
| `POST /api/v1/admin/site-metrics/visits/reset` | `PRINCIPAL` + CSRF | Resets the school visit counter to zero. |

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

School branding uses the same storage boundary. A logo is stored under `branding/<school-slug>/` with a generated filename; the database retains its object key and the school response exposes only a configured public URL. Replacing or removing a logo updates the database before best-effort cleanup of the previous ImageKit object, so a cleanup failure cannot leave the portal pointing to a missing logo.

Standard 3 Ekam Kasoti import is available through the documented pre-enrolled student roster mapping and never reads or stores Aadhaar data. Result publication, available-result choices, individual lookup, other exam formats, and timetables still require their own approved contracts.

All `/api/v1/admin/**` responses use `Cache-Control: no-store`.

## Principal-facing terminology

The backend keeps `Assessment` as the generic domain, DTO, and route concept so it can support
multiple exam formats without a schema rename. The principal-facing admin UI calls this area
**Results & Marks** and calls an individual assessment an **Exam / Test**. Result upload and public
lookup are explicitly unavailable until the documented privacy-safe roll-number and PIN workflow is
approved; publishing an Exam / Test currently publishes setup metadata only.

## Gallery lifecycle

The album is the gallery publication boundary. Draft albums are absent from public routes. Publishing
an album publishes all of its non-archived images; an image uploaded later to that album is published
automatically. Unpublishing returns both the album and its published images to draft state, so public
gallery APIs cannot expose them.

Image order is assigned server-side as `1..N`. Batch upload preserves multipart file order. Reorder
requests must name every image in the target album exactly once; duplicate, missing, or foreign image
IDs are rejected before any update. The database unique constraint remains in force while the service
uses a temporary non-conflicting range during order changes.

An album uses an image in the same album as its cover. The first image becomes the default cover;
deleting the cover selects the next image in display order, and deleting the last image clears it.
ImageKit deletion occurs before metadata deletion. Storage failures are returned to the caller so the
database reference remains available for retry rather than being silently removed.

Album list DTOs include `coverImageThumbnailUrl` and `imageCount`. Admin lists count every image
the principal can manage; public lists count only published images and return a cover thumbnail only
when that cover is published. This lets album cards render without per-album image-list requests.
