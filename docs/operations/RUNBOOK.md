# Operations Runbook — Rorfost School Platform

## Incident Response Matrix

| Incident | Immediate Action | Mitigation & Follow-up |
|---|---|---|
| **Service Not Responding** | Check Cloudflare Worker status, Render dashboard logs, and Neon DB connectivity. | If recent release caused regression, roll back to prior release SHA. |
| **Gateway Token Compromised** | Deactivate compromised token row in `portal_gateway_credentials` (`is_active = false`). | Generate new 256-bit token offline, insert SHA-256 digest in DB, update Cloudflare Worker secret (`PORTAL_GATEWAY_TOKEN`), deploy Worker. |
| **Cross-Tenant Data Leak Alert** | Immediately suspend affected school's gateway token to fail closed. | Review application logs, isolate query predicates, patch repository filters, and verify via negative test suite before restoring service. |
| **Database Pool Exhausted** | Inspect active connections in Neon. Verify Render HikariCP bounds (`DB_POOL_MAX_SIZE=3`). | Terminate orphaned connections; adjust idle connection timeout if necessary. |
| **Accidentally Published Results** | Execute admin unpublish action for the assessment. | Confirm public lookup returns empty/not found; inspect audit log for actor details. |
| **Corrupted Result Workbook Upload** | Re-upload valid workbook with same `resultType` (transactional replace) or delete via admin. | Verify marksheet viewer displays correct values; inspect Apache POI parsing logs. |
| **ImageKit Quota Warning** | Audit storage usage by school prefix (`schools/<uuid>/`). | Remove unused draft images or orphaned assets under approved retention policy. |
| **Principal Locked Out** | Verify email and school slug binding in `admin_users`. | Reset password via operator-approved secure command; never reveal account existence publicly. |
| **Rollback Release** | Deploy known-good commit SHA on Render and point Cloudflare Workers to compatible version. | Validate schema compatibility before any database alterations. |
