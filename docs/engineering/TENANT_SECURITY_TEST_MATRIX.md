# Tenant Security Test Matrix

**Sprint 24 deliverable** — Comprehensive endpoint × tenant × actor evidence document.  
All tests listed here are automated and live in the `backend/src/test` source tree.  
Legend: ✅ positive (success path) · ❌ negative (must fail closed) · 🔒 isolation (cross-tenant barrier)

---

## 1. Public School & Profile Endpoints

| Endpoint | Actor | Token / Condition | Expected | Test Class |
|---|---|---|---|---|
| `GET /api/v1/public/school` | Visitor | Token A | 200 — School A data | `SchoolTenantIsolationTest` |
| `GET /api/v1/public/school` | Visitor | Token B | 200 — School B data | `SchoolTenantIsolationTest` |
| `GET /api/v1/public/school` | Visitor | Token B + spoofed slug/schoolId params | 200 — School B only, A data ignored | `SchoolTenantIsolationTest`, `ForgedHeaderRequestTest` |
| `GET /api/v1/public/school` | Visitor | No token | 403 `missing_gateway_credential` | `SchoolTenantIsolationTest`, `ForgedHeaderRequestTest` |
| `GET /api/v1/public/school` | Visitor | Unknown token | 403 `invalid_gateway_credential` | `SchoolTenantIsolationTest`, `ForgedHeaderRequestTest` |
| `GET /api/v1/public/school` | Visitor | Inactive-tenant token | 403 `invalid_gateway_credential` | `SchoolTenantIsolationTest`, `ForgedHeaderRequestTest` |
| `GET /api/v1/public/school` | Visitor | Blank/whitespace token | 403 `missing_gateway_credential` | `ForgedHeaderRequestTest` |
| `GET /api/v1/public/school` | Visitor | Token C + schoolId A param | 200 — School C data, not A | `ForgedHeaderRequestTest` |
| `GET /api/v1/public/principal-profile` | Visitor | Token A (profile public) | 200 — Profile A | `SchoolTenantIsolationTest` |
| `GET /api/v1/public/principal-profile` | Visitor | Token B (no profile) | 404 `principal_profile_not_found` | `SchoolTenantIsolationTest` |
| `GET /api/v1/public/principal-profile` | Visitor | Token B (profile private) | 404 `principal_profile_not_found` | `SchoolTenantIsolationTest` |

---

## 2. Visit Counter Endpoints

| Endpoint | Actor | Token / Condition | Expected | Test Class |
|---|---|---|---|---|
| `POST /api/v1/public/visits` | Visitor | Token A (×3) | count A increments to 3 | `VisitTenantIsolationTest` |
| `POST /api/v1/public/visits` | Visitor | Token B (×1) | count B stays at 1 | `VisitTenantIsolationTest` |
| `GET /api/v1/public/visits` | Visitor | Token A after 3 increments | count = 3 | `VisitTenantIsolationTest` |
| `GET /api/v1/public/visits` | Visitor | Token B after 1 increment | count = 1 | `VisitTenantIsolationTest` |
| `POST /api/v1/public/visits` | Visitor | No token | 403 | `VisitTenantIsolationTest` |
| `POST /api/v1/public/visits` | Visitor | Inactive token | 403 | `VisitTenantIsolationTest` |
| `DELETE /api/v1/admin/visits` | Admin A | Token A, Admin A session | resets A counter; B unaffected | `VisitTenantIsolationTest` |
| `DELETE /api/v1/admin/visits` | Admin A | Token B, Admin A session | 403 cross_tenant | `VisitTenantIsolationTest` |

---

## 3. Public Content Endpoints (Gallery, Notices, Materials)

| Endpoint | Actor | Token / Condition | Expected | Test Class |
|---|---|---|---|---|
| `GET /api/v1/public/gallery/albums` | Visitor | Token A | School A albums only | `PublicContentTenantIsolationTest` |
| `GET /api/v1/public/gallery/albums` | Visitor | Token B | School B albums only, not A | `PublicContentTenantIsolationTest` |
| `GET /api/v1/public/notices` | Visitor | Token A | School A notices | `PublicContentTenantIsolationTest` |
| `GET /api/v1/public/notices` | Visitor | Token B | School B notices, not A | `PublicContentTenantIsolationTest` |
| `GET /api/v1/public/materials` | Visitor | Token A | School A materials | `PublicContentTenantIsolationTest` |
| `GET /api/v1/public/materials` | Visitor | Token B | School B materials, not A | `PublicContentTenantIsolationTest` |
| `GET /api/v1/public/gallery/albums` | Visitor | Inactive school | 0 albums | `PublicContentTenantIsolationTest` |

---

## 4. Admin Content Endpoints (CRUD, Upload, Delete, Clear)

| Endpoint | Actor | Token / Condition | Expected | Test Class |
|---|---|---|---|---|
| `GET /api/v1/admin/gallery/albums` | Admin A | Token A, session A | School A albums | `AdminContentTenantIsolationTest` |
| `POST /api/v1/admin/gallery/albums` | Admin A | Token A, session A | Creates album under School A | `AdminContentTenantIsolationTest` |
| `PUT /api/v1/admin/gallery/albums/{id}` | Admin A | Token A, session A, School B's album ID | 403 / 404 — cannot mutate B | `AdminContentTenantIsolationTest` |
| `DELETE /api/v1/admin/gallery/albums/{id}` | Admin A | Token A, session A, School B's album ID | 403 / 404 | `AdminContentTenantIsolationTest` |
| `DELETE /api/v1/admin/gallery/albums/{id}` | Admin B | Token B, session B, School A's album ID | 403 / 404 | `AdminContentTenantIsolationTest` |

---

## 5. Public Academic Lookup Endpoints

| Endpoint | Actor | Token / Condition | Expected | Test Class |
|---|---|---|---|---|
| `GET /api/v1/public/academic/classes` | Visitor | Token A | School A classes only | `PublicAcademicTenantIsolationTest` |
| `GET /api/v1/public/academic/classes` | Visitor | Token B | School B classes, not A | `PublicAcademicTenantIsolationTest` |
| `GET /api/v1/public/academic/subjects` | Visitor | Token A | School A subjects | `PublicAcademicTenantIsolationTest` |
| `GET /api/v1/public/academic/subjects` | Visitor | Token B | School B subjects, not A | `PublicAcademicTenantIsolationTest` |
| `GET /api/v1/public/academic/*` | Visitor | No token | 403 | `PublicAcademicTenantIsolationTest` |

---

## 6. Exam Results & Presentation Settings

| Endpoint | Actor | Token / Condition | Expected | Test Class |
|---|---|---|---|---|
| `GET /api/v1/public/results` | Visitor | Token A, valid roll/class | School A results | `ExamResultTenantIsolationTest` |
| `GET /api/v1/public/results` | Visitor | Token B, School A's roll number | 404 — roll not in B | `ExamResultTenantIsolationTest` |
| `GET /api/v1/admin/results/presentation` | Admin A | Token A, session A | School A settings | `ExamResultTenantIsolationTest` |
| `PUT /api/v1/admin/results/presentation` | Admin A | Token B, session A | 403 cross_tenant | `ExamResultTenantIsolationTest` |
| `GET /api/v1/public/results` | Visitor | No token | 403 | `ExamResultTenantIsolationTest` |
| `GET /api/v1/public/results` | Visitor | Cache-Control header check | no-store | `ExamResultTenantIsolationTest` |

---

## 7. Assessment (Marks Workbook) Endpoints

| Endpoint | Actor | Token / Condition | Expected | Test Class |
|---|---|---|---|---|
| `GET /api/v1/admin/assessments` | Admin A | Token A, session A | School A assessments | `AssessmentTenantIsolationTest` |
| `PUT /api/v1/admin/assessments/{id}` | Admin A | Token A, session A, School B's assessment ID | 403 / 404 | `AssessmentTenantIsolationTest` |
| `DELETE /api/v1/admin/assessments/{id}` | Admin B | Token B, session B, School A's assessment ID | 403 / 404 | `AssessmentTenantIsolationTest` |

---

## 8. Feature Flag Endpoints

| Endpoint | Actor | Token / Condition | Expected | Test Class |
|---|---|---|---|---|
| `GET /api/v1/public/features` | Visitor | Token A (GALLERY=on) | GALLERY=true | `PublicFeatureFlagTenantIsolationTest` |
| `GET /api/v1/public/features` | Visitor | Token B (GALLERY=off) | GALLERY=false | `PublicFeatureFlagTenantIsolationTest` |
| `GET /api/v1/public/features` | Visitor | No token | 403 | `PublicFeatureFlagTenantIsolationTest` |
| `GET /api/v1/public/features` | Visitor | Unknown token | 403 | `PublicFeatureFlagTenantIsolationTest` |

---

## 9. Admin Login, Password Change & CSRF

| Endpoint | Actor | Token / Condition | Expected | Test Class |
|---|---|---|---|---|
| `POST /api/v1/admin/auth/login` | Admin A | Token A, correct credentials | 200 — session for A | `AdminAuthenticationTenantIsolationTest` |
| `POST /api/v1/admin/auth/login` | Admin A | Token B, School A credentials | 401 — credentials match A but portal is B | `AdminAuthenticationTenantIsolationTest` |
| `POST /api/v1/admin/auth/login` | Admin A | Token A, wrong password | 401 | `AdminAuthenticationControllerTest` |
| `POST /api/v1/admin/auth/change-password` | Admin A | Token A, session A | 200 — password changed | `AdminAuthenticationTenantIsolationTest` |
| `POST /api/v1/admin/auth/change-password` | Admin A | Token B, session A | 403 cross_tenant | `AdminAuthenticationTenantIsolationTest` |
| `GET /api/v1/admin/auth/csrf` | Any | Token A | CSRF token returned | `AdminAuthenticationControllerTest` |

---

## 10. Session Tenant Binding & Forged Headers

| Scenario | Token / Condition | Expected | Test Class |
|---|---|---|---|
| Admin session schoolId matches portal TenantContext | Token A, session A | Allowed | `AdminSessionTenantBindingFilterTest` |
| Admin session schoolId mismatches portal TenantContext | Token B, session A | 403 `cross_tenant_access_denied` | `AdminSessionTenantBindingFilterTest`, `SessionPortalIsolationTest` |
| Admin endpoint with no session (unauthenticated) | Token A, no session | 4xx — denied | `SessionPortalIsolationTest` |
| must-change-password session, cross-tenant portal | Token B, session A (mustChangePwd=true) | 403 `cross_tenant_access_denied` | `SessionPortalIsolationTest` |
| Two admin sessions, each on correct portal | Token A+A, Token B+B (concurrent) | Both 200 — no data cross-observation | `SessionPortalIsolationTest` |
| No gateway token (direct-to-Render) | No token | 403 `missing_gateway_credential` | `ForgedHeaderRequestTest` |
| Blank/whitespace token | `   ` | 403 `missing_gateway_credential` | `ForgedHeaderRequestTest` |
| Forged token | `totally-forged-credential` | 403 `invalid_gateway_credential` | `ForgedHeaderRequestTest` |
| Inactive/revoked tenant token | `TOKEN_INACTIVE` | 403 `invalid_gateway_credential` | `ForgedHeaderRequestTest` |
| Token C + schoolId A in query param | Token C, `?schoolId=<A>` | 200 — School C data only | `ForgedHeaderRequestTest` |
| Token A + spoofed slug=school-b in params | Token A + params | 200 — School A data only | `ForgedHeaderRequestTest` |
| Health endpoint bypasses gateway filter | `/actuator/health`, no token | not 403 | `ForgedHeaderRequestTest` |

---

## 11. Storage / Media Delete Ownership

| Scenario | Expected | Test Class |
|---|---|---|
| Admin A deletes own school's object key | Allowed | `CrossTenantMediaDeletionProtectionTest` |
| Admin A deletes School B's object key (canonical path `schools/<B-uuid>/...`) | SecurityException — key does not belong to school A | `CrossTenantMediaDeletionProtectionTest` |
| Admin A deletes legacy branding path for School A | Allowed | `StorageServiceTest` |
| Admin A deletes legacy branding path for School B | SecurityException | `CrossTenantMediaDeletionProtectionTest` |

---

## 12. Concurrency / ThreadLocal Leak Property Tests

| Scenario | Expected | Test Class |
|---|---|---|
| 8 threads × 20 iterations, School A/B interleaved | Every response contains only its own tenant's school name | `CrossTenantConcurrencyTest` |
| TenantContextHolder: fresh request on same thread has no context | `getTenantContext` returns empty | `CrossTenantConcurrencyTest` |
| Setting context on request 2 does not mutate request 1 | Context on request 1 unchanged | `CrossTenantConcurrencyTest` |
| Three-tenant fixture: A/B/C IDs and slugs are all disjoint | All UUIDs and slugs distinct; sessions embed correct schoolId | `CrossTenantConcurrencyTest` |

---

## 13. Gateway Credential Filter (Unit)

| Scenario | Expected | Test Class |
|---|---|---|
| Valid token resolves tenant and sets request attribute | 200 / context bound | `GatewayCredentialAuthenticationFilterTest` |
| Missing token | 403 `missing_gateway_credential` | `GatewayCredentialAuthenticationFilterTest` |
| Invalid token | 403 `invalid_gateway_credential` | `GatewayCredentialAuthenticationFilterTest` |
| Gateway disabled (transitional mode) | Request passes without token | `GatewayCredentialAuthenticationFilterTest` |
| OPTIONS preflight | Passes without token | `GatewayCredentialAuthenticationFilterTest` |
| `/actuator/health` | Passes without token | `GatewayCredentialAuthenticationFilterTest` |

---

## Endpoint Families — Coverage Summary

| Family | Positive Tests | Negative (Cross-Tenant) | Missing / Future |
|---|---|---|---|
| Public school / profile | ✅ | ✅ | — |
| Public visits | ✅ | ✅ | — |
| Public content (gallery/notices/materials) | ✅ | ✅ | — |
| Public academic lookup | ✅ | ✅ | — |
| Public exam results | ✅ | ✅ | — |
| Public feature flags | ✅ | ✅ | — |
| Admin auth (login/CSRF/change-pwd) | ✅ | ✅ | — |
| Admin content (gallery/notices/materials CRUD) | ✅ | ✅ | — |
| Admin assessment / marks workbook | ✅ | ✅ | — |
| Admin session binding | ✅ | ✅ | — |
| Forged header / origin bypass | — | ✅ | — |
| Storage / media delete ownership | ✅ | ✅ | — |
| Concurrency / ThreadLocal leak | ✅ | ✅ | — |
| Admin visit reset | ✅ | ✅ | — |

> **Status:** All listed endpoint families have at least one positive test and at least one negative
> cross-tenant test. No unchecked endpoint families exist as of Sprint 24.

---

*Last updated: Sprint 24 — Cross-tenant automated test suite*
