# Tenant Package Architecture Map

This package boundary is established as part of the multi-tenancy foundation (Sprint 01).
No runtime classes are added in this sprint to preserve strict nonfunctional scope.

## Structure and Implementation Status
 
- `com.rorfost.schoolportal.tenant.domain`:
  - `SchoolGatewayCredential` *(Implemented in Sprint 03)* — Entity representing SHA-256 token digests, active status, rotation metadata, and school binding.
  - `TenantContext` *(Planned for Sprint 05)* — Immutable request-scoped context containing verified `schoolId`, `slug`, and tenant state.
- `com.rorfost.schoolportal.tenant.repository`:
  - `SchoolGatewayCredentialRepository` *(Implemented in Sprint 03)* — Fast lookups and active token verification by SHA-256 digest.
- `com.rorfost.schoolportal.tenant.application`:
  - `TenantContextHolder` *(Planned for Sprint 05)* — ThreadLocal / request-attribute holder for the current tenant.
  - `TenantResolutionService` *(Planned for Sprint 05)* — Resolves and verifies gateway tokens.
- `com.rorfost.schoolportal.tenant.security`:
  - `PortalGatewayFilter` *(Planned for Sprint 05)* — Intercepts `/api/*` requests, extracts `X-Portal-Gateway-Token`, verifies hash, and populates `TenantContext`.

## Current State
- Schema: `school_gateway_credentials` created via `V25__create_school_gateway_credentials.sql` (Sprint 03).
- Model: `SchoolGatewayCredential` and `SchoolGatewayCredentialRepository` provide cryptographic digest generation and lookup without exposing public endpoints.
- Single-school operational mode remains unchanged. Public and admin controllers currently resolve the first active school via `SchoolRepository.findFirstByIsActiveTrueOrderByCreatedAtAsc()`. Transition to `TenantContext` occurs in Sprint 05.
