# Multi-Tenant Rollout Plan and Checklist

This runbook outlines the planned 30-sprint migration from a single-school portal to the multi-tenant **Rorfost School Platform**.

## Rollout Sequence & Stages

| Stage | Sprints | Focus Area | Mandatory Gate |
|---|---|---|---|
| **1. Structure & Baseline** | 01–02 | Package boundaries, generic platform docs, reproducible baseline tests | Render and Cloudflare auto-deploy frozen |
| **2. Secure Routing** | 03–05 | Tenant credential schema, Cloudflare Worker gateway token, Render filter | Cross-tenant forged header tests pass |
| **3. Public Isolation** | 06–10 | Isolation of school info, content, academic, exam results, and visits | Multi-tenant test fixtures return distinct data |
| **4. Principal Isolation** | 11–14 | School-scoped login, session binding, CRUD/media/audit isolation | Principal of School A cannot access or mutate School B |
| **5. Onboarding & Flags**| 15–18 | Operator provisioning runbook, feature flag schema, API & React gates | Disabled features blocked at backend & UI |
| **6. Frontend & Storage**| 19–21 | Dynamic branding defaults, per-school Cloudflare Workers, ImageKit paths | School 1 portal visually and functionally unchanged |
| **7. Migration Planning** | 22–23 | Neon DB preparation, Aiven→Neon dry run, parity validation | Verified dump/restore without PII leakage |
| **8. Assurance** | 24–27 | Negative tenant suite, resource validation, staging 3-school E2E | Manual release CI verified; owner sign-off |
| **9. Production Cutover**| 28–29 | Maintenance window, frozen data cutover to Neon, School 2/3 onboarding | Human authorization, rollback ready |
| **10. Final Audit** | 30 | Dead code cleanup, secret rotation, production security audit | Formal production release acceptance |

## Mandatory Human Gates

1. **Auto-Deploy Freeze:** Prior to any deployment-impacting push, verify Render Auto-Deploy is OFF and Cloudflare production builds are detached or set to non-promoting commands.
2. **Data Privacy Guard:** No production database dumps, real student PII, or credentials may be loaded into AI tooling or committed to repository tracking.
3. **Gateway Verification:** Never activate a shared tenant database without verified gateway authentication and strict data isolation.
4. **Capacity & SLA Realism:** Free-tier limits (Render Free 750h/month, Neon Free compute/storage, Cloudflare 100k requests/day) are bounded; monitor resource utilization closely.

## Rollback Principles

- **Pre-cutover:** Immediate reversion to existing Render deployment commit and Aiven DB configuration.
- **Post-cutover (before writes):** Point Cloudflare Workers back to old backend/database.
- **Post-cutover (after writes):** Requires data reconciliation plan; never execute blind rollback that drops new school data.
