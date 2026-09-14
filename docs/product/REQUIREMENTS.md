# Requirements

## Functional requirements

| ID | Requirement |
| --- | --- |
| FR-001 | The portal shall show public school, principal, contact, notice, gallery, and Student Corner content without login. |
| FR-002 | A principal with the `PRINCIPAL` role shall eventually manage school configuration and public content. |
| FR-003 | The portal shall support academic years, standards, subjects, standard-subject mappings, and generic assessments. |
| FR-004 | The portal shall support study materials, downloads, notice attachments, gallery images, and timetable files through object storage with database metadata. |
| FR-005 | Imported results shall be draft by default and require explicit principal publication. |
| FR-006 | Public result lookup shall return only one individual result and shall never publicly return a complete class list. |
| FR-007 | Result responses shall use `Cache-Control: no-store`; PINs shall not be stored in plaintext. |
| FR-008 | The Excel workbook contract is pending. Parsing, validation fields, and import mapping shall not be implemented or specified until the real workbook/template is reviewed. |

## Non-functional requirements

| ID | Requirement |
| --- | --- |
| NFR-001 | Use Spring Security, server-side JDBC sessions, secure cookies, CSRF protection, explicit CORS, and least-data collection. |
| NFR-002 | Support keyboard access, semantic HTML, labels, focus visibility, useful alt text, contrast, logical headings, and touch-friendly controls. |
| NFR-003 | Design public pages mobile-first, then tablet and desktop. |
| NFR-004 | Keep a simple modular monolith that is maintainable by a small team and suitable for 100–500 users. |
| NFR-005 | Use PostgreSQL/Flyway for relational data and an S3-compatible store for files; do not store binary files in PostgreSQL. |
| NFR-006 | Maintain auditable admin and sensitive bulk actions, backups, recovery checks, and production-safe logging. |
| NFR-007 | Use Gujarati-first, simple, natural UI copy for all visible user interactions. |
| NFR-008 | Validate upload type and size server-side. Exact allowed types and limits are an implementation-time policy decision to document before enabling uploads. |
| NFR-009 | Keep deployment portable across standard JVM/container hosts; avoid provider-specific application logic. |
| NFR-010 | Do not commit secrets or personal/school data; protect backups and private result data. |
