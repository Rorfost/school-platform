# Tenant Package Architecture Map

This package boundary is established as part of the multi-tenancy foundation (Sprint 01).
No runtime classes are added in this sprint to preserve strict nonfunctional scope.

## Target Structure (Sprints 03–05)

- `com.rorfost.schoolportal.tenant.domain`:
  - `TenantContext` — Immutable request-scoped context containing verified `schoolId`, `slug`, and tenant state.
  - `PortalGatewayCredential` — Entity representing portal gateway token digests, active status, and school binding.
- `com.rorfost.schoolportal.tenant.repository`:
  - `PortalGatewayCredentialRepository` — Lookup by SHA-256 token hash.
- `com.rorfost.schoolportal.tenant.application`:
  - `TenantContextHolder` — ThreadLocal / request-attribute holder for the current tenant.
  - `TenantResolutionService` — Resolves and verifies gateway tokens.
- `com.rorfost.schoolportal.tenant.security`:
  - `PortalGatewayFilter` — Intercepts `/api/*` requests, extracts `X-Portal-Gateway-Token`, verifies hash, and populates `TenantContext`.

## Current State (As-Is)
Single-school operational mode. Public and admin controllers currently resolve the first active school via `SchoolRepository.findFirstByIsActiveTrueOrderByCreatedAtAsc()`. Transition to `TenantContext` occurs incrementally in Sprints 05–14.
