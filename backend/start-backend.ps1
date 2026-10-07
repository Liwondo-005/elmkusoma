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
    [switch]$NoWindow
)

$ErrorActionPreference = "Stop"
$backendDir = $PSScriptRoot

# --- configuration: backend/.env is the single source of truth ---------------
$envFile = Join-Path $backendDir ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | Where-Object { $_ -match '^[A-Za-z_][A-Za-z0-9_]*=' } | ForEach-Object {
        $key, $value = $_ -split '=', 2
        Set-Item -Path "env:$key" -Value $value
    }
} else {
    Write-Host "WARNING: backend/.env not found - falling back to service defaults." -ForegroundColor Yellow
}

# The media service talks to MinIO in production. Local dev has no MinIO, so it
# uses the filesystem store (application.yml documents MEDIA_STORAGE=local).
if (-not $env:MEDIA_STORAGE) { $env:MEDIA_STORAGE = "local" }

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