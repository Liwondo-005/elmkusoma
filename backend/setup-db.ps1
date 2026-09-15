# =============================================================================
# ELMKUSOMA Database Setup Script (Windows)
# =============================================================================
# Run this ONCE to create the PostgreSQL database.
# Requires: PostgreSQL installed and running
#
# Usage:  .\setup-db.ps1
#    or:  .\setup-db.ps1 -PgUser postgres -DbName elmkusoma
# =============================================================================

param(
    [string]$PgUser = "postgres",
    [string]$DbName = "elmkusoma",
    [string]$PgBin = ""
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ELMKUSOMA Database Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Auto-detect psql path if not provided
if (-not $PgBin) {
    $candidates = @(
        "C:\Program Files\PostgreSQL\18\bin",
        "C:\Program Files\PostgreSQL\17\bin",
        "C:\Program Files\PostgreSQL\16\bin",
        "C:\Program Files\PostgreSQL\15\bin"
    )
    foreach ($path in $candidates) {
        if (Test-Path "$path\psql.exe") {
            $PgBin = $path
            break
        }
    }
    # Also check PATH
    if (-not $PgBin) {
        $PgBin = (Get-Command psql -ErrorAction SilentlyContinue).Source | Split-Path
    }
}

if (-not $PgBin) {
    Write-Host "ERROR: PostgreSQL not found. Install PostgreSQL or specify -PgBin parameter." -ForegroundColor Red
    exit 1
}

$psql = Join-Path $PgBin "psql.exe"
if (-not (Test-Path $psql)) {
    Write-Host "ERROR: psql.exe not found at $PgBin" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Using PostgreSQL at: $PgBin" -ForegroundColor Yellow
Write-Host "Creating database: $DbName" -ForegroundColor Yellow

# Build SQL
$sql = @"
SELECT 'CREATE DATABASE $DbName'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$DbName')\gexec

GRANT ALL PRIVILEGES ON DATABASE $DbName TO $PgUser;
"@

# Write SQL to temp file and execute
$tempSql = Join-Path $env:TEMP "elmkusoma_setup.sql"
$sql | Set-Content -Path $tempSql -Encoding UTF8

try {
    & $psql -U $PgUser -d postgres -f $tempSql 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "psql failed with exit code $LASTEXITCODE"
    }

    # Schema permissions
    $schemaSql = @"
\connect $DbName
GRANT ALL ON SCHEMA public TO $PgUser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $PgUser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO $PgUser;
"@
    $schemaSql | Set-Content -Path $tempSql -Encoding UTF8
    & $psql -U $PgUser -d $DbName -f $tempSql 2>&1

    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  Setup Complete!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Database: $DbName" -ForegroundColor White
    Write-Host "User:     $PgUser" -ForegroundColor White
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "  1. Copy .env.example to .env" -ForegroundColor White
    Write-Host "  2. Set DB_PASSWORD in .env to your PostgreSQL password" -ForegroundColor White
    Write-Host "  3. Start: cd backend\elmkusoma-core && mvnw spring-boot:run" -ForegroundColor White
    Write-Host ""

} catch {
    Write-Host ""
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "If password authentication failed, ensure PostgreSQL is running" -ForegroundColor Yellow
    Write-Host "and try running from pgAdmin or with your PostgreSQL password:" -ForegroundColor Yellow
    Write-Host "  psql -U postgres -d postgres -f $tempSql" -ForegroundColor White
    exit 1
} finally {
    Remove-Item -Path $tempSql -ErrorAction SilentlyContinue
}
