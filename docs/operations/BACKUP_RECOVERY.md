# Backup and Recovery

Use managed PostgreSQL backups plus periodic verified exports appropriate to the school’s data volume. Before destructive or bulk result operations, take or confirm an important database backup. R2 objects need their own retention/export plan; a database backup alone does not restore files.

Keep more than one copy of important data, protect backup credentials/encryption, and never place backups with production/student data in source control. Periodically perform a restore verification in a safe environment: restore database, validate expected records, verify referenced storage objects, and record the result.

Recovery checklist: identify the last known-good point; stop harmful writes; choose the backup; restore to a safe target; verify database and object consistency; validate admin/public behavior; communicate the recovery scope; then document root cause and prevention.
