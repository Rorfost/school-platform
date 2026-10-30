# Frontend Tenant Feature Architecture Map

This directory is reserved for tenant-specific context, hooks, and utilities (Sprint 01).

## Target Structure (Sprints 18–20)

- `TenantContext.tsx` — Context providing active school branding, slug, and gateway awareness.
- `useTenant.ts` — Hook to consume current tenant metadata without hardcoded school assumptions.
- `tenantConfig.ts` — Dynamic configuration derived from verified tenant responses.

## Current State (As-Is)

Multi-tenant school branding and generic frontend defaults implemented (Sprint 19). Fallback metadata is generic neutral, document title syncs dynamically with the active school name, and the footer dynamically reflects attributed school data and separate platform software credit. Multi-tenant worker awareness is scheduled for Sprint 20.
