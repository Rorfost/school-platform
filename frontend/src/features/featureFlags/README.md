# Frontend Feature Flags Architecture Map

This directory is reserved for client-side feature flag integration (Sprint 01).

## Target Structure (Sprint 18)

- `FeatureFlagContext.tsx` — Fetches and supplies tenant-specific feature flags from `/api/v1/public/features`.
- `useFeatureFlag.ts` — Hook for components to query feature availability.
- `FeatureGate.tsx` — Declarative wrapper component to conditionally render UI elements based on active flags.

## Current State (As-Is)

All UI features and navigation items are unconditionally visible. Sprints 16–18 introduce dynamic backend enforcement and frontend feature awareness.
