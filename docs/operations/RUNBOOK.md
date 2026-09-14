# Small Operations Runbook

| Situation | Initial action |
| --- | --- |
| App not responding | Check Cloudflare, backend health/logs, recent deployment, and database reachability; roll back a known-bad release if necessary. |
| Database unavailable | Check managed database status/credentials/network, avoid write retries that can duplicate work, then restore only under the backup plan. |
| Object storage unavailable | Pause uploads/publishing that need storage; preserve metadata changes only when safe; check R2/MinIO endpoint and credentials. |
| Bad deployment | Stop or roll back the release, confirm health and a smoke test, then investigate without exposing data in logs. |
| Result accidentally published | Unpublish immediately through the future audited admin action, confirm public lookup is unavailable, and record the incident. |
| Incorrect bulk import | Keep results unpublished, stop further imports, identify the import/audit record, and use the approved rollback workflow. |
| Admin cannot log in | Verify account status, session/cookie configuration, allowed origin, and password-reset process; never reveal account existence publicly. |
| Storage nearing limits | Review largest/obsolete objects under an approved retention policy; avoid deleting referenced or legally required files. |
| Restore previous release | Select a known-good build, deploy it, verify health/critical flows, and assess migration compatibility before any database rollback. |
