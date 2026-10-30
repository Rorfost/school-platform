.SYNOPSIS
    Exports a PostgreSQL database using pg_dump custom format (-Fc).
.DESCRIPTION
    Creates a custom format dump with ownership and ACLs stripped.
    NOTE: The resulting dump file is NOT automatically encrypted. An encryption step
    (e.g. gpg / openssl) must actually be executed before offsite or external storage.
.PARAMETER OutputFile
    Path where the dump file should be saved.
.PARAMETER SourceDbUrl
    Connection string (or reads from $env:SOURCE_DB_URL).
.EXAMPLE
    .\backup.ps1 -OutputFile "C:\backups\db.dump" -SourceDbUrl "postgresql://user:pass@host/db?sslmode=require"
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [string]$OutputFile,

    [Parameter()]
    [string]$SourceDbUrl = $env:SOURCE_DB_URL
)

$ErrorActionPreference = "Stop"

if (-not $SourceDbUrl -and -not $env:PGHOST) {
    Write-Error "ERROR: Neither -SourceDbUrl nor `$env:SOURCE_DB_URL / `$env:PGHOST is defined."
    exit 1
}

$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
if (-not $OutputFile) {
    $BackupDir = if ($env:BACKUP_DIR) { $env:BACKUP_DIR } else { ".\backups" }
    if (-not (Test-Path $BackupDir)) {
        New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null
    }
    $OutputFile = Join-Path $BackupDir "school_portal_$Timestamp.dump"
} else {
    $ParentDir = Split-Path $OutputFile -Parent
    if ($ParentDir -and -not (Test-Path $ParentDir)) {
        New-Item -ItemType Directory -Path $ParentDir -Force | Out-Null
    }
}

Write-Host "==> Starting database export to: $OutputFile"

$PgDumpArgs = @(
    "--format=custom",
    "--no-owner",
    "--no-acl",
    "--file=$OutputFile"
)

if ($SourceDbUrl) {
    $PgDumpArgs += "--dbname=$SourceDbUrl"
}

& pg_dump @PgDumpArgs
if ($LASTEXITCODE -ne 0) {
    Write-Error "pg_dump failed with exit code $LASTEXITCODE"
    exit $LASTEXITCODE
}

$FileSize = (Get-Item $OutputFile).Length / 1MB
Write-Host ("==> Backup complete: {0} ({1:N2} MB)" -f $OutputFile, $FileSize)
Write-Host "IMPORTANT: Encrypt this backup before storing offsite."
