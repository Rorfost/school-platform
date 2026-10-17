# Feature Flag Package Architecture Map

This package boundary is established as part of the multi-tenancy foundation (Sprint 01).
No runtime classes are added in this sprint to preserve strict nonfunctional scope.

## Target Structure (Sprints 16–17)

- `com.rorfost.schoolportal.featureflag.domain`:
  - `FeatureKey` — Enumeration of platform features (e.g., `GALLERY`, `RESULTS_EKAM_KASOTI`, `RESULTS_ANNUAL`, `STUDY_MATERIALS`).
  - `SchoolFeature` — Entity mapping `(school_id, feature_key, enabled)`.
- `com.rorfost.schoolportal.featureflag.repository`:
  - `SchoolFeatureRepository` — Persistence queries for school-specific feature overrides.
- `com.rorfost.schoolportal.featureflag.application`:
  - `FeatureFlagService` — Query feature state with fallback to default policy.
- `com.rorfost.schoolportal.featureflag.api`:
  - `PublicFeatureFlagController` — Returns enabled feature list for the requesting tenant.
  - Interceptors / annotations for feature enforcement on backend endpoints.

## Current State (As-Is)
All features are currently enabled globally or hardcoded. School-scoped feature flags will be phased in during Sprints 16–18.
