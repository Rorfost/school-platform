<#
.SYNOPSIS
    Restores a custom format pg_dump export into a target PostgreSQL database.
.DESCRIPTION
    Executes pg_restore with clean, if-exists, no-owner, and no-acl flags.
.PARAMETER DumpFile
    Path to the .dump file to restore.
.PARAMETER TargetDbUrl
    Connection string for the target database.
.EXAMPLE
    .\restore.ps1 -DumpFile "C:\backups\db.dump" -TargetDbUrl "postgresql://user:pass@ep-staging.us-east-2.aws.neon.tech/neondb?sslmode=require"
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$DumpFile,

    [Parameter(Position = 1)]
    [string]$TargetDbUrl = $env:TARGET_DB_URL,

    [switch]$ForceProd
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $DumpFile)) {
    Write-Error "ERROR: Dump file not found: $DumpFile"
    exit 1
}

if (-not $TargetDbUrl -and -not $env:PGHOST) {
    Write-Error "ERROR: Target database connection string not specified. Set -TargetDbUrl or `$env:TARGET_DB_URL."
    exit 1
}

if ($TargetDbUrl -match "prod" -and -not $ForceProd -and $env:ALLOW_PROD_RESTORE -ne "1") {
    Write-Error "SAFETY ERROR: Target URL appears to reference 'prod'. Set -ForceProd or `$env:ALLOW_PROD_RESTORE=1 to override."
    exit 1
}

Write-Host "==> Restoring from $DumpFile..."

$PgRestoreArgs = @(
    "--clean",
    "--if-exists",
    "--no-owner",
    "--no-acl"
)

if ($TargetDbUrl) {
    $PgRestoreArgs += "--dbname=$TargetDbUrl"
}

$PgRestoreArgs += $DumpFile

& pg_restore @PgRestoreArgs
$Code = $LASTEXITCODE
if ($Code -eq 0 -or $Code -eq 1) {
    Write-Host "==> Database restore finished successfully (exit code $Code)."
    Write-Host "==> Run .\verify_parity.ps1 to validate table counts and schema integrity."
} else {
    Write-Error "ERROR: pg_restore failed with exit code $Code"
    exit $Code
}
