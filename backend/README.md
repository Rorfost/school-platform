# Backend — Rorfost School Platform

The backend is a Java 21 / Spring Boot 4.x Maven modular monolith providing REST APIs, relational persistence via PostgreSQL, JDBC-backed principal sessions, BCrypt authentication, CSRF/CORS/security-header controls, audit logging, request IDs, and an ImageKit-backed object-storage abstraction.

## As-Is vs. Target Architecture

### Current State (As-Is)
- Single-school deployment (reference: Dhadhana Primary School) transitioning to multi-tenancy.
- Public school identity, principal profile, public content endpoints (materials, notices, albums, album images, downloads), public academic lookups (standards, subjects, current academic year), exam results / presentation settings (public marks lookup, admin Excel upload, clear results, signatures/grading policies), and visit counters / metrics strictly resolve and verify the school via request-scoped `TenantContext`, `SchoolRepository.findByIdAndIsActiveTrue(schoolId)`, and authenticated `PrincipalSession`.
- All public endpoints across the modular monolith are now fully tenant-isolated; subsequent sprints address principal login multi-tenancy, session binding, and onboarding.
- Database: PostgreSQL with Flyway migrations V1–V25.
- Storage: ImageKit single account/bucket.

### Target State (Multi-Tenant Plan)
- Multi-tenant architecture serving multiple schools on a shared Render Free backend.
- Request-scoped `TenantContext` populated by a cryptographic gateway filter (`X-Portal-Gateway-Token` verification).
- Strict `school_id` isolation across all domain services, repositories, and child entity relationships.
- Database: Neon PostgreSQL with shared schema and append-only Flyway migrations.
- Media: ImageKit with school-partitioned directory prefixes (`schools/<school-uuid>/...`).
- Feature enforcement: School-level feature flags via `com.rorfost.schoolportal.featureflag`.

## Commands

```bash
# Run locally with local profile
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Run unit tests
mvn test

# Run full verification (including Testcontainers if Docker is active)
mvn verify

# Check and apply Spotless code style
mvn spotless:check
mvn spotless:apply
```

Use Java 21 as the supported baseline. Configuration uses environment variables with `.env.example` as a template.

## Package Boundaries

### Existing Core Packages
- `com.rorfost.schoolportal.common`: Configuration, security, exceptions, and ImageKit storage abstraction.
- `com.rorfost.schoolportal.auth`: Principal authentication, session restore, password lifecycle, and login rate limiting.
- `com.rorfost.schoolportal.school`: School branding, principal profile, settings, and visitor metrics.
- `com.rorfost.schoolportal.academic`: Academic years, standards, subjects, standard-subject mappings, and student roster.
- `com.rorfost.schoolportal.assessment`: Annual and Ekam Kasoti examination results, marksheet calculation, and presentation settings.
- `com.rorfost.schoolportal.content`: Notices, study materials, photo gallery, and official downloads.
- `com.rorfost.schoolportal.dashboard`: Administrative summary and metrics aggregation.
- `com.rorfost.schoolportal.audit`: Append-only administrative event logging.

### Planned Multi-Tenant Packages (Sprint 01 Foundation)
- `com.rorfost.schoolportal.tenant`: Tenant context holder, gateway credential entity, repository, and filter (Sprints 03–05).
- `com.rorfost.schoolportal.featureflag`: School feature entity, registry, service, and endpoint enforcement (Sprints 16–17).
