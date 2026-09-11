# ELMKUSOMA Backend Start Script
# Starts PostgreSQL (if needed), loads .env, and runs the Spring Boot application

param(
    [switch]$SkipDbCheck
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Join-Path $scriptDir "elmkusoma-core"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ELMKUSOMA Backend" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 1. Load .env file if present
$envFile = Join-Path $scriptDir ".env"
if (Test-Path $envFile) {
    Write-Host "Loading .env..." -ForegroundColor Yellow
    Get-Content $envFile | ForEach-Object {
        if ($_ -match "^\s*([^#][^=]+)=(.+)$") {
            $key = $Matches[1].Trim()
            $val = $Matches[2].Trim()
            [Environment]::SetEnvironmentVariable($key, $val, "Process")
        }
    }
} else {
    Write-Host "No .env file found. Using defaults." -ForegroundColor Yellow
    Write-Host "Copy .env.example to .env to customize." -ForegroundColor Gray
}

# 2. Set defaults if not in env
if (-not $env:DB_USERNAME) { $env:DB_USERNAME = "elmkusoma" }
if (-not $env:DB_PASSWORD) { $env:DB_PASSWORD = "changeme" }
if (-not $env:DB_HOST)     { $env:DB_HOST = "localhost" }
if (-not $env:DB_PORT)     { $env:DB_PORT = "5432" }
if (-not $env:DB_NAME)     { $env:DB_NAME = "elmkusoma" }
if (-not $env:JWT_SECRET)  { $env:JWT_SECRET = "Y2hvb3NlYS1hLXNlY3VyZS1zZWNyZXQta2V5LWZvci1lbG1rdXNvbWEtand0LXRva2VuLWdlbmVyYXRpb24tMjAyNA==" }

$env:DB_USERNAME = $env:DB_USERNAME
$env:DB_PASSWORD = $env:DB_PASSWORD

# 3. Check Java
$javaHome = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
if (Test-Path "$javaHome\bin\java.exe") {
    $env:JAVA_HOME = $javaHome
    $env:PATH = "$javaHome\bin;$env:PATH"
} else {
    # Try system java
    $javaVer = & java -version 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Java not found. Install JDK 21." -ForegroundColor Red
        exit 1
    }
}

Write-Host "Java: $((& java -version 2>&1 | Select-Object -First 1))" -ForegroundColor Green

# 4. Check PostgreSQL
if (-not $SkipDbCheck) {
    $pgService = Get-Service postgresql-x64-18 -ErrorAction SilentlyContinue
    if ($pgService -and $pgService.Status -ne "Running") {
        Write-Host "Starting PostgreSQL..." -ForegroundColor Yellow
        Start-Service postgresql-x64-18
        Start-Sleep -Seconds 3
    }
    if ($pgService) {
        Write-Host "PostgreSQL: Running" -ForegroundColor Green
    } else {
        Write-Host "WARNING: PostgreSQL service not found. Is it installed?" -ForegroundColor Yellow
    }
}

# 5. Build if needed
$jar = Join-Path $backendDir "target\elmkusoma-core-0.1.0-SNAPSHOT.jar"
if (-not (Test-Path $jar)) {
    Write-Host "Building backend..." -ForegroundColor Yellow
    Push-Location $backendDir
    & mvn package "-DskipTests" "-Dmaven.test.skip=true"
    Pop-Location
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Build failed!" -ForegroundColor Red
        exit 1
    }
}

# 6. Start the app
Write-Host ""
Write-Host "Starting ELMKUSOMA on http://localhost:8080 ..." -ForegroundColor Green
Write-Host "Press Ctrl+C to stop" -ForegroundColor Gray
Write-Host ""

Push-Location $backendDir
& java -jar $jar
Pop-Location
