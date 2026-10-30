# Tenant Package Architecture Map

This package boundary is established as part of the multi-tenancy foundation (Sprint 01).
No runtime classes are added in this sprint to preserve strict nonfunctional scope.

## Structure and Implementation Status
 
- `com.rorfost.schoolportal.tenant.domain`:
  - `SchoolGatewayCredential` *(Implemented in Sprint 03)* — Entity representing SHA-256 token digests, active status, rotation metadata, and school binding.
  - `TenantContext` *(Implemented in Sprint 05)* — Immutable request-scoped context containing verified `schoolId` and `slug`.
- `com.rorfost.schoolportal.tenant.repository`:
  - `SchoolGatewayCredentialRepository` *(Implemented in Sprint 03)* — Fast lookups and active token verification by SHA-256 digest.
- `com.rorfost.schoolportal.tenant.application`:
  - `TenantContextHolder` *(Implemented in Sprint 05)* — Request-scoped accessor ensuring strict thread and concurrency isolation.
  - `TenantResolver` *(Implemented in Sprint 05)* — Constant-time token verification, active school validation, and tenant resolution.
- `com.rorfost.schoolportal.tenant.security`:
  - `GatewayCredentialAuthenticationFilter` *(Implemented in Sprint 05)* — Intercepts `/api/*` requests, extracts `X-Portal-Gateway-Token`, verifies hash, and binds `TenantContext`.

## Current State
- Schema: `school_gateway_credentials` created via `V25__create_school_gateway_credentials.sql` (Sprint 03).
- Model & Security: `GatewayCredentialAuthenticationFilter` and `TenantResolver` validate incoming edge proxy tokens on `/api/*` routes and fail closed on unauthorized requests.
- Anonymous Exemption: Minimal `/actuator/health` bypasses token validation without leaking tenant configuration.
- Controller Transition: Incremental migration of individual domain controllers to use `TenantContext` begins in Sprint 06.
