# Result Import Contract and Safety — Rorfost School Platform

## Overview & Multi-Tenant Boundaries

The result import workflow enables school principals to upload approved spreadsheet workbooks (`.xlsx`) to populate student marks for Annual and Ekam Kasoti examinations.

### As-Is vs. Target Multi-Tenant Behavior
- **Current State (As-Is):** The upload service resolves the active school via `SchoolRepository.findFirstByIsActiveTrueOrderByCreatedAtAsc()`.
- **Target State (Sprint 09):** The upload endpoint accepts the authenticated principal's `TenantContext.schoolId` and transactionally deletes and replaces only results for that specific school. Uploading results for School A cannot affect School B.

## Import Principles & Security

1. **Strict Authentication & CSRF:** Uploads are restricted to authenticated administrators (`ROLE_PRINCIPAL`) on same-origin endpoints with valid CSRF tokens.
2. **Transactional Replacement:** An upload for a given `resultType` (`ANNUAL` or `EKAM_KASOTI`) replaces existing rows for that type within the active school in a single database transaction. If validation fails, changes are completely rolled back.
3. **No Government Identifiers:** The portal strictly prohibits reading, logging, hashing, or storing government identifiers (such as Aadhaar UID). Student identity begins with school-assigned General Register Number (`generalRegisterNumber`), Standard, and Roll Number.
4. **Draft vs. Published Lifecycle:** Uploaded results are draft until explicit publication, and public lookups require student standard and roll number with `Cache-Control: no-store`.

## Approved Shared Workbook Layouts

### 1. Annual Exam Result (વાર્ષિક પરીક્ષા પરિણામ)
- Sheet contains headers on Row 0 and data starting from Row 1.
- Column 0: G.R. No. (જી.આર. નં)
- Column 1: Standard (ધોરણ)
- Column 2: Student Name (વિદ્યાર્થીનું નામ)
- Column 3: Birth Date (જન્મ તારીખ)
- Column 4: Attended Days (હાજર દિવસ)
- Columns 5+: Subject Mark & Grade pairs (Gujarati, Maths, Science, Hindi, English, Social Science, Sanskrit, Personality Development, Environment).
- Admin supplies `totalWorkingDays` (કુલ કાર્ય દિવસ) during upload.

### 2. Ekam Kasoti Result (એકમ કસોટી પરિણામ)
- Same structure as Annual Exam, but lacks Attended Days column; subject marks begin at Column 4.
- Total working days is not collected or required.

## Testing & Fixture Rules

All automated tests and local verification must use synthetic mock workbooks containing fictitious student names and numbers. Real student records or production marksheet files must never be committed to the repository or uploaded to AI tools.
