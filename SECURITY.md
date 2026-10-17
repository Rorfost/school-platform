# Security Policy — Rorfost School Platform

## Reporting a Vulnerability

Please report security issues privately to the project owner through the repository's established contact channels. Do not open public GitHub issues containing credentials, session IDs, gateway tokens, production URLs, or confidential school/student data.

Provide a minimal, sanitized reproduction without real student records, result workbooks, or database exports.

## Supported Versions

Security fixes are applied to the active default branch and designated production deployments. Older revisions or unsupported experimental branches are not separately maintained.

## Threat Model & Boundary Guidelines

### Current State (As-Is)
- Single-school portal with Spring Security and JDBC-backed sessions.
- CSRF protection required on state-changing administrative requests.
- Strict `Cache-Control: no-store` on administrative and sensitive result endpoints.

### Target State (Multi-Tenant Hardening)
- **Zero Trust in Unauthenticated Routing:** Render backend must never trust unauthenticated `Host`, `Origin`, `X-Forwarded-Host`, `X-School-Id`, or client-injected headers for tenant authorization.
- **Gateway Authentication:** Tenant context is established strictly through a cryptographic `PORTAL_GATEWAY_TOKEN` shared securely between the Cloudflare Worker and the backend filter.
- **Session-Tenant Binding:** Admin sessions must explicitly bind to the tenant context (`PrincipalSession.schoolId == TenantContext.schoolId`). Cross-school session reuse is blocked.
- **Isolation by Default:** All database queries, uploads, and deletions must be scoped by `school_id`.

## Repository Safety Rules

Never commit passwords, API tokens, private keys, session cookies, database credentials, or real student PII to this repository. All test suites must use synthetic mock fixtures.
