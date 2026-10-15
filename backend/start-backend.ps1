# Starts the ELMKUSOMA backend services for local development.
#
#   .\start-backend.ps1            # core (:8080) + media (:8083)
#   .\start-backend.ps1 -CoreOnly  # core only
#
# Previous versions hard-coded another machine's paths (C:\Users\dell\...), a
# database password and a JWT secret, and started only the core service. The
# frontend proxies /api/v1/media/** to the media service, so with core alone the
# Media Library failed with an opaque "Server returned non-JSON response (500)"
# (Next returns a bare 500 when the upstream is down). Paths and credentials now
# come from this file's location and from backend/.env.

param(
    [switch]$CoreOnly,
    [switch]$NoWindow,
    [switch]$VerifyConfig
)

$ErrorActionPreference = "Stop"
$backendDir = $PSScriptRoot

# --- configuration: backend/.env is the single source of truth ---------------
$envFile = Join-Path $backendDir ".env"

# Strips one layer of matching surrounding quotes, mirroring the application
# loader in ElmkusomaCoreApplication.unquote. The payment gateway key is matched
# by its sk_test_/sk_live_ prefix, so a quoted value would stop matching and the
# sandbox would be rejected as "not a Stakaba key".
function ConvertFrom-DotenvValue {
    param([string]$Value)
    if ($null -eq $Value) { return $Value }
    $v = $Value.Trim()
    if ($v.Length -ge 2 -and ($v[0] -eq '"' -or $v[0] -eq "'") -and $v[$v.Length - 1] -eq $v[0]) {
        return $v.Substring(1, $v.Length - 2)
    }
    return $v
}

if (Test-Path $envFile) {
    Get-Content $envFile | Where-Object { $_ -match '^\s*[A-Za-z_][A-Za-z0-9_]*\s*=' } | ForEach-Object {
        $key, $value = $_ -split '=', 2
        Set-Item -Path "env:$($key.Trim())" -Value (ConvertFrom-DotenvValue $value)
    }
} else {
    Write-Host "WARNING: backend/.env not found - falling back to service defaults." -ForegroundColor Yellow
}

# The media service talks to MinIO in production. Local dev has no MinIO, so it
# uses the filesystem store (application.yml documents MEDIA_STORAGE=local).
if (-not $env:MEDIA_STORAGE) { $env:MEDIA_STORAGE = "local" }

# --- -VerifyConfig: report what the application WILL see, print no secrets ----
if ($VerifyConfig) {
    # Never prints a value. Reports only whether a variable is set, plus the key's
    # mode derived from its prefix - the same classification StakabaProperties uses,
    # so "SANDBOX" here genuinely means the app will classify it as a sandbox key.
    function Get-RedactedState {
        param([string]$Name)
        $raw = [Environment]::GetEnvironmentVariable($Name, "Process")
        if ([string]::IsNullOrWhiteSpace($raw)) { return "not set" }
        $v = $raw.Trim()
        if ($v.StartsWith("sk_test_")) { return "set (sandbox key sk_test_*)" }
        if ($v.StartsWith("sk_live_")) { return "set (LIVE key sk_live_*)" }
        if ($Name -eq "STAKABA_API_KEY") { return "set (does not match sk_test_*/sk_live_*)" }
        return "set"
    }

    Write-Host ""
    Write-Host "  ELMKUSOMA configuration check (no values printed)" -ForegroundColor Cyan
    Write-Host "  -----------------------------------------------" -ForegroundColor Cyan
    Write-Host ("  .env in use          : {0}" -f $(if (Test-Path $envFile) { $envFile } else { "NOT FOUND - backend/.env" }))
    Write-Host ("  DB_PASSWORD          : {0}" -f (Get-RedactedState "DB_PASSWORD"))
    Write-Host ("  JWT_SECRET           : {0}" -f (Get-RedactedState "JWT_SECRET"))
    Write-Host ("  provider             : {0}" -f $(if ($env:ELMKUSOMA_PAYMENT_PROVIDER) { $env:ELMKUSOMA_PAYMENT_PROVIDER } else { "not set (legacy manual verification)" }))
    Write-Host ("  STAKABA_ENABLED      : {0}" -f $(if ($env:STAKABA_ENABLED) { $env:STAKABA_ENABLED } else { "not set" }))
    Write-Host ("  STAKABA_BASE_URL     : {0}" -f $(if ($env:STAKABA_BASE_URL) { $env:STAKABA_BASE_URL } else { "not set" }))
    Write-Host ("  STAKABA_API_KEY      : {0}" -f (Get-RedactedState "STAKABA_API_KEY"))
    Write-Host ("  STAKABA_ALLOW_LIVE   : {0}" -f $(if ($env:STAKABA_ALLOW_LIVE) { $env:STAKABA_ALLOW_LIVE } else { "not set (defaults false)" }))
    Write-Host ""

    if (-not (Test-Path $envFile)) {
        Write-Host "  Next: Copy-Item .env.example .env, then fill in your values." -ForegroundColor Yellow
        exit 1
    }
    if ($env:STAKABA_ENABLED -eq "true" -and -not $env:STAKABA_API_KEY) {
        Write-Host "  BLOCKED: STAKABA_ENABLED=true but STAKABA_API_KEY is empty." -ForegroundColor Red
        Write-Host "  The application will refuse to start. Add your sandbox key." -ForegroundColor Yellow
        exit 1
    }
    Write-Host "  Configuration parsed. Start the app with .\start-backend.ps1" -ForegroundColor Green
    exit 0
}

# --- locate java -------------------------------------------------------------
if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $java = "$env:JAVA_HOME\bin\java.exe"
} else {
    $java = (Get-Command java -ErrorAction SilentlyContinue).Source
}
if (-not $java) {
    Write-Host "ERROR: java not found. Set JAVA_HOME or add java to PATH." -ForegroundColor Red
    exit 1
}

function Start-ServiceJar {
    param(
        [string]$Name,
        [string]$ModuleDir,
        [string]$MainClass
    )

    $jar = Get-ChildItem -Path (Join-Path $ModuleDir "target") -Filter "$Name-*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch "sources|javadoc|original" } |
        Select-Object -First 1

    $windowArgs = if ($NoWindow) { @("-Xmx256m", "-jar", $jar.FullName) } else { @("-Xmx256m", "-jar", $jar.FullName) }

    if ($jar) {
        Write-Host "Starting $Name from $($jar.Name)..." -ForegroundColor Green
        Start-Process -FilePath $java -ArgumentList $windowArgs -WorkingDirectory $ModuleDir -WindowStyle Minimized
        return
    }

    # No packaged jar (fresh checkout): run from target\classes with a
    # dependency classpath. Either name works:
    #   elmk_cp.txt / media_cp.txt                        (hand-saved)
    #   mvn dependency:build-classpath "-Dmdep.outputFile=%TEMP%\<module>_cp.txt"
    $short = if ($Name -eq "elmkusoma-media") { "media" } else { "elmk" }
    $classpathFile = @(
        (Join-Path $env:TEMP "$short`_cp.txt"),
        (Join-Path $env:TEMP "$Name`_cp.txt")
    ) | Where-Object { Test-Path $_ } | Select-Object -First 1

    $classes = Join-Path $ModuleDir "target\classes"
    if (-not (Test-Path $classes)) {
        Write-Host "ERROR: $ModuleDir\target\classes missing. Run: (cd $ModuleDir; mvn -o compile)" -ForegroundColor Red
        return
    }
    if (-not $classpathFile) {
        Write-Host "ERROR: no classpath file for $Name in $env:TEMP. Run:" -ForegroundColor Yellow
        Write-Host "  (cd $ModuleDir; mvn -o dependency:build-classpath \"-Dmdep.outputFile=$env:TEMP\${short}_cp.txt\")" -ForegroundColor Yellow
        return
    }
    $cp = ((Get-Content $classpathFile -Raw).Trim() + ";" + $classes)
    Write-Host "Starting $Name from target\classes..." -ForegroundColor Green
    Start-Process -FilePath $java -ArgumentList @("-Xmx256m", "-cp", $cp, $MainClass) -WorkingDirectory $ModuleDir -WindowStyle Minimized
}

Start-ServiceJar -Name "elmkusoma-core" -ModuleDir (Join-Path $backendDir "elmkusoma-core") -MainClass "tz.elmkusoma.ElmkusomaCoreApplication"

if (-not $CoreOnly) {
    Start-ServiceJar -Name "elmkusoma-media" -ModuleDir (Join-Path $backendDir "elmkusoma-media") -MainClass "tz.elmkusoma.ElmkusomaMediaApplication"
}

Write-Host ""
Write-Host "  core  : http://localhost:8080" -ForegroundColor Cyan
if (-not $CoreOnly) {
    Write-Host "  media : http://localhost:8083   (MEDIA_STORAGE=$env:MEDIA_STORAGE)" -ForegroundColor Cyan
}
Write-Host "  web   : npm run dev  (frontend\package.json)" -ForegroundColor Cyan