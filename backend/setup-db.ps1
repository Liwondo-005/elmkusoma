# ELMKUSOMA Database Setup Script
# Run this ONCE as Administrator to create the PostgreSQL database and user.
# Requires: PostgreSQL 18+ installed and running

param(
    [string]$PgUser = "postgres",
    [string]$AppUser = "elmkusoma",
    [string]$AppPassword = "changeme",
    [string]$DbName = "elmkusoma",
    [string]$PgBin = "C:\Program Files\PostgreSQL\18\bin"
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ELMKUSOMA Database Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Check PostgreSQL is running
$pgService = Get-Service postgresql-x64-18 -ErrorAction SilentlyContinue
if ($pgService.Status -ne "Running") {
    Write-Host "Starting PostgreSQL..." -ForegroundColor Yellow
    Start-Service postgresql-x64-18
    Start-Sleep -Seconds 3
}

# Check psql exists
$psql = Join-Path $PgBin "psql.exe"
if (-not (Test-Path $psql)) {
    Write-Host "ERROR: psql.exe not found at $psql" -ForegroundColor Red
    Write-Host "Please adjust -PgBin parameter or install PostgreSQL." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Creating user '$AppUser' and database '$DbName'..." -ForegroundColor Yellow

# Build SQL
$sql = @"
DO `$`$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '$AppUser') THEN
        CREATE ROLE $AppUser WITH LOGIN PASSWORD '$AppPassword' CREATEDB;
    END IF;
END
$$;

SELECT 'CREATE DATABASE $DbName OWNER $AppUser'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$DbName')\gexec

GRANT ALL PRIVILEGES ON DATABASE $DbName TO $AppUser;
ALTER DATABASE $DbName OWNER TO $AppUser;
"@

# Write SQL to temp file and execute
$tempSql = Join-Path $env:TEMP "elmkusoma_setup.sql"
$sql | Set-Content -Path $tempSql -Encoding UTF8

try {
    # Try connecting with the provided superuser password
    & $psql -U $PgUser -d postgres -f $tempSql 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "psql failed with exit code $LASTEXITCODE"
    }

    # Now grant schema-level permissions
    $schemaSql = @"
\connect $DbName
GRANT ALL ON SCHEMA public TO $AppUser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $AppUser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO $AppUser;
"@
    $schemaSql | Set-Content -Path $tempSql -Encoding UTF8
    & $psql -U $PgUser -f $tempSql 2>&1

    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  Setup Complete!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Database: $DbName" -ForegroundColor White
    Write-Host "User:     $AppUser" -ForegroundColor White
    Write-Host "Password: $AppPassword" -ForegroundColor White
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "  1. Copy .env.example to .env and update values" -ForegroundColor White
    Write-Host "  2. Run: .\start.ps1" -ForegroundColor White
    Write-Host ""

} catch {
    Write-Host ""
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "If password authentication failed, try running this script" -ForegroundColor Yellow
    Write-Host "from pgAdmin or manually with your postgres password:" -ForegroundColor Yellow
    Write-Host "  psql -U postgres -d postgres -f $tempSql" -ForegroundColor White
    exit 1
} finally {
    Remove-Item -Path $tempSql -ErrorAction SilentlyContinue
}
