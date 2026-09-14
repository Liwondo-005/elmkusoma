# ELMKUSOMA Database Setup Script
# Run this ONCE to create the PostgreSQL database.
# All developers use the same database name and username.
# Only the password differs per developer.
#
# Requires: PostgreSQL installed and running
# Usage: powershell -ExecutionPolicy Bypass -File setup-db.ps1

param(
    [string]$PgUser = "postgres",
    [string]$DbName = "elmkusoma",
    [string]$PgBin = ""
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ELMKUSOMA Database Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Auto-detect psql path
if ($PgBin -eq "") {
    $candidates = @(
        "C:\Program Files\PostgreSQL\18\bin",
        "C:\Program Files\PostgreSQL\17\bin",
        "C:\Program Files\PostgreSQL\16\bin",
        "C:\Program Files\PostgreSQL\15\bin"
    )
    foreach ($c in $candidates) {
        if (Test-Path "$c\psql.exe") { $PgBin = $c; break }
    }
}

$psql = if ($PgBin) { Join-Path $PgBin "psql.exe" } else { "psql" }

# Check psql exists
if ($PgBin -and -not (Test-Path $psql)) {
    Write-Host "ERROR: psql.exe not found at $psql" -ForegroundColor Red
    Write-Host "Please install PostgreSQL or pass -PgBin parameter." -ForegroundColor Red
    exit 1
}

# Check PostgreSQL is running
$pgService = Get-Service postgresql-x64-* -ErrorAction SilentlyContinue | Where-Object { $_.Status -eq "Running" } | Select-Object -First 1
if ($pgService) {
    Write-Host "PostgreSQL: Running ($($pgService.Name))" -ForegroundColor Green
} else {
    Write-Host "WARNING: No running PostgreSQL service found." -ForegroundColor Yellow
    Write-Host "Make sure PostgreSQL is running before proceeding." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Creating database '$DbName' for user '$PgUser'..." -ForegroundColor Yellow
Write-Host ""

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

    # Grant schema permissions
    $schemaSql = @"
\connect $DbName
GRANT ALL ON SCHEMA public TO $PgUser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $PgUser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO $PgUser;
"@
    $schemaSql | Set-Content -Path $tempSql -Encoding UTF8
    & $psql -U $PgUser -f $tempSql 2>&1

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
    Write-Host "  2. Set your local PostgreSQL password in .env" -ForegroundColor White
    Write-Host "  3. Run: .\start.ps1" -ForegroundColor White
    Write-Host ""

} catch {
    Write-Host ""
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Make sure PostgreSQL is running and you can connect as '$PgUser'." -ForegroundColor Yellow
    Write-Host "Try manually: psql -U $PgUser -d postgres" -ForegroundColor White
    exit 1
} finally {
    Remove-Item -Path $tempSql -ErrorAction SilentlyContinue
}
