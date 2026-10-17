# Frontend Tenant Feature Architecture Map

This directory is reserved for tenant-specific context, hooks, and utilities (Sprint 01).

## Target Structure (Sprints 18–20)

- `TenantContext.tsx` — Context providing active school branding, slug, and gateway awareness.
- `useTenant.ts` — Hook to consume current tenant metadata without hardcoded school assumptions.
- `tenantConfig.ts` — Dynamic configuration derived from verified tenant responses.

## Current State (As-Is)
Single school deployment mode. `useSchoolData.ts` and `Footer.tsx` contain Dhadhana-specific default fallbacks. Dynamic branding and multi-tenant worker awareness are scheduled for Sprints 19–20.
