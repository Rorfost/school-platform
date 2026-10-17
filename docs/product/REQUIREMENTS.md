# Requirements — Rorfost School Platform

## Functional Requirements

| ID | Requirement | Status |
|---|---|---|
| **FR-001** | The portal shall show public school, principal, contact, notice, gallery, and Student Corner content without login. | Baseline Complete |
| **FR-002** | A principal with role `PRINCIPAL` shall manage school configuration, academic setup, and public content. | Baseline Complete |
| **FR-003** | The portal shall support academic years, standards, subjects, standard-subject mappings, and generic assessments. | Baseline Complete |
| **FR-004** | The portal shall support study materials, downloads, notice attachments, and photo galleries via ImageKit object storage. | Baseline Complete |
| **FR-005** | Imported exam results shall be draft by default and require explicit publication by the principal. | Baseline Complete |
| **FR-006** | Public result lookup shall return only one individual result and shall never expose class-wide marks. | Baseline Complete |
| **FR-007** | Result responses shall enforce `Cache-Control: no-store`; result PINs shall be stored as BCrypt hashes. | Baseline Complete |
| **FR-008** | Excel import shall parse Annual and Ekam Kasoti workbooks using school-controlled roll numbers and G.R. numbers, never reading or storing Aadhaar data. | Baseline Complete |
| **FR-009** | **Multi-Tenant Gateway Routing:** The backend shall verify incoming `X-Portal-Gateway-Token` from trusted Cloudflare Workers to resolve request `TenantContext`. | Planned (Sprints 03–05) |
| **FR-010** | **Data Isolation:** All database queries, uploads, and deletions shall be strictly isolated by `school_id`. No cross-tenant access shall be permitted. | Planned (Sprints 06–14) |
| **FR-011** | **School-Scoped Authentication:** Principal login shall authenticate against `(school_id, email)` and bind sessions to the tenant context. | Planned (Sprints 11–12) |
| **FR-012** | **Feature Flag Registry:** The platform shall support school-specific feature toggles enforced at the backend and reflected in the frontend. | Planned (Sprints 16–18) |

## Non-Functional Requirements

| ID | Requirement |
|---|---|
| **NFR-001** | Use Spring Security, server-side JDBC sessions, Secure/HttpOnly/SameSite cookies, CSRF protection, and explicit CORS. |
| **NFR-002** | Support keyboard access, semantic HTML, visible focus states, useful alt text, contrast, and touch-friendly controls. |
| **NFR-003** | Mobile-first responsive design for primary school students, parents, and principals. |
| **NFR-004** | Maintain a cohesive modular monolith suitable for 3–10 schools within free-tier provider limits. |
| **NFR-005** | Use PostgreSQL with append-only Flyway migrations and ImageKit for file storage; do not store binary blobs in PostgreSQL. |
| **NFR-006** | Maintain auditable admin actions, verified backup/recovery procedures, and error sanitization. |
| **NFR-007** | Use natural, clear, simple Gujarati copy for all public student and parent interactions. |
| **NFR-008** | Validate file upload types (signature check) and size ceilings server-side. |
| **NFR-009** | Keep backend code portable across standard JVM/container hosts without cloud-provider lock-in. |
| **NFR-010** | Protect student privacy: never commit secrets, credentials, or student records to source control. |
| **NFR-011** | Zero trust in unauthenticated client tenant headers (`X-School-Id`, `Host`, `Origin`). |
| **NFR-012** | Conservative database connection pool sizing (`DB_POOL_MAX_SIZE=3`) to accommodate free-tier resource limits. |
| **NFR-013** | Negative cross-tenant security test coverage verifying tenant isolation prior to release. |
| **NFR-014** | Source-available licensing posture with accurate contributor notices and school content separation. |
