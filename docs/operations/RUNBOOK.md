# Operations Runbook — Rorfost School Platform

## Incident Response Matrix

| Incident                                      | Immediate Action                                                                                                                                        | Mitigation & Follow-up                                                                                                                                  |
| --------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Service Not Responding / Cold Start Delay** | Check Cloudflare Worker status, Render dashboard logs, and Neon DB connectivity. Verify if Render instance is spinning up from idle (typically 40–50s). | Client applies 45s timeout with Gujarati loading/retry indicators. No writes are replayed automatically. If regression, roll back to prior release SHA. |
| **Gateway Token Compromised**                 | Deactivate compromised token row in `portal_gateway_credentials` (`is_active = false`).                                                                 | Generate new 256-bit token offline, insert SHA-256 digest in DB, update Cloudflare Worker secret (`PORTAL_GATEWAY_TOKEN`), deploy Worker.               |
| **Cross-Tenant Data Leak Alert**              | Immediately suspend affected school's gateway token to fail closed.                                                                                     | Review application logs, isolate query predicates, patch repository filters, and verify via negative test suite before restoring service.               |
| **Database Pool Exhausted**                   | Inspect active connections in Neon. Verify Render HikariCP bounds (`DB_POOL_MAX_SIZE=3`).                                                               | Terminate orphaned connections; adjust idle connection timeout if necessary.                                                                            |
| **Accidentally Published Results**            | Execute admin unpublish action for the assessment.                                                                                                      | Confirm public lookup returns empty/not found; inspect audit log for actor details.                                                                     |
| **Corrupted Result Workbook Upload**          | Re-upload valid workbook with same `resultType` (transactional replace) or delete via admin.                                                            | Verify marksheet viewer displays correct values; inspect Apache POI parsing logs.                                                                       |
| **ImageKit Quota Warning / 429 Rate Limit**   | Audit bandwidth and media transformation counts in ImageKit dashboard.                                                                                  | Check client cache behavior; purge expired or orphan thumbnails. Client falls back to natural Gujarati rate limit message without retrying writes.      |
| **Principal Locked Out**                      | Verify email and school slug binding in `admin_users`.                                                                                                  | Reset password via operator-approved secure command; never reveal account existence publicly.                                                           |
| **Rollback Release**                          | Deploy known-good commit SHA on Render and point Cloudflare Workers to compatible version.                                                              | Validate schema compatibility before any database alterations.                                                                                          |

---

## Health Check and Liveness Contract

- **Endpoint:** `/actuator/health`
- **Exposure:** Restricted to `show-details: never`. The endpoint outputs only high-level status (`{"status":"UP"}`) without leaking database connection strings, credentials, disk layouts, or tenant metadata.
- **Monitoring Guidance:** External uptime pings (e.g., UptimeRobot, BetterUptime) can monitor `/actuator/health` to detect total service outages, but **pings do not represent an SLA** and must not trigger automated paid cloud upgrades or bypass normal cold start error handling.

---

## Recovery Metrics & Operational Drill Targets (Sprint 29 Verified)

- **Target RPO (Recovery Point Objective):** ~15 minutes (Neon automated continuous WAL streaming and daily encrypted off-site dumps).
- **Target RTO (Recovery Time Objective):** ~10–20 minutes on Render Free/Neon Free infrastructure.
- **Measured Recovery Drill Results:**
    - Nonproduction DB restore: **3m 45s**
    - Gateway credential rotation: **1m 20s**
    - Emergency tenant deactivation: **< 1s**
    - Asset manifest recovery: **2m 10s**
    - Application release rollback: **2m 30s**
