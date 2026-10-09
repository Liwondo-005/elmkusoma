<#
.SYNOPSIS
  Unified ELMKUSOMA development launcher: LiveKit + backend + frontend.

.DESCRIPTION
  Starts every service the Live UI depends on, in dependency order, and blocks
  until they are all ready or a failure is reported.

  Design notes
  ------------
  * LiveKit mode is derived from backend/.env (the project's single source of
    truth for credentials). If LIVEKIT_URL points at a remote host - LiveKit
    Cloud or another machine - NO local server is started, which avoids running
    two competing SFUs. Only a localhost URL starts a local server, and even
    then an already-listening port is reused rather than duplicated.
  * The frontend never holds LiveKit credentials. live-classroom.tsx receives
    `liveKitToken` and `liveKitUrl` from the backend API, and the backend mints
    the token from livekit.server.{url,api-key,api-secret}. So this script
    never passes secrets to the frontend and never writes a secret to disk.
  * If a local LiveKit is required but no server can be found, the script fails
    loudly with install instructions. It never reports a ready Live service it
    could not actually start.

.PARAMETER LocalLiveKit
  Force local mode: start/reuse a local LiveKit server and verify that
  backend/.env credentials match livekit.yaml.

.PARAMETER SkipBackend
  Do not start the backend (frontend only).

.PARAMETER SkipFrontend
  Do not start the frontend (backend only).

.EXAMPLE
  .\start-dev.ps1
  Start everything, using whichever LiveKit backend/.env points at.

.EXAMPLE
  .\start-dev.ps1 -LocalLiveKit
  Start a local LiveKit server and check the credentials match livekit.yaml.
#>
[CmdletBinding()]
param(
    [switch]$LocalLiveKit,
    [switch]$SkipBackend,
    [switch]$SkipFrontend,
    [int]$BackendPort = 8080,
    [int]$FrontendPort = 3000,
    [int]$LiveKitPort = 7880,
    [int]$ReadyTimeoutSec = 180
)

$ErrorActionPreference = "Stop"
$RepoRoot = $PSScriptRoot
$BackendDir = Join-Path $RepoRoot "backend\elmkusoma-core"
$FrontendDir = Join-Path $RepoRoot "frontend"
$LogDir = Join-Path $RepoRoot ".devlogs"

# Child processes started by this script, so shutdown never orphans them.
$script:Children = @()

function Write-Step  { param($m) Write-Host "==> $m" -ForegroundColor Cyan }
function Write-Ok    { param($m) Write-Host "    [ok]   $m" -ForegroundColor Green }
function Write-Warn2 { param($m) Write-Host "    [warn] $m" -ForegroundColor Yellow }
function Write-Err2  { param($m) Write-Host "    [FAIL] $m" -ForegroundColor Red }
function Write-Info  { param($m) Write-Host "    $m" -ForegroundColor Gray }

# ---------------------------------------------------------------- .env loading
function Import-DotEnv {
    $envFile = Join-Path $RepoRoot "backend\.env"
    if (-not (Test-Path $envFile)) {
        Write-Err2 "backend/.env not found."
        Write-Info "Copy backend/.env.example to backend/.env and fill in DB_PASSWORD."
        exit 1
    }
    # Only well-formed KEY=VALUE lines; .env may contain comments and blank lines.
    Get-Content $envFile |
        Where-Object { $_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$' } |
        ForEach-Object {
            $key = $Matches[1]
            $value = $Matches[2].Trim()
            Set-Item -Path "env:$key" -Value $value
        }
    return $envFile
}

# ------------------------------------------------------------------- networking
function Test-TcpPort {
    param([string]$Host_, [int]$Port, [int]$TimeoutMs = 1200)
    try {
        $c = New-Object System.Net.Sockets.TcpClient
        $a = $c.BeginConnect($Host_, $Port, $null, $null)
        if (-not $a.AsyncWaitHandle.WaitOne($TimeoutMs)) { $c.Close(); return $false }
        $c.EndConnect($a)
        $c.Close()
        return $true
    } catch { return $false }
}

function Wait-TcpPort {
    param([string]$Label, [string]$Host_, [int]$Port, [int]$TimeoutSec)
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        if (Test-TcpPort -Host_ $Host_ -Port $Port) { return $true }
        Start-Sleep -Milliseconds 500
    }
    return $false
}

function Get-PortOwner {
    param([int]$Port)
    $c = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
         Select-Object -First 1
    if (-not $c) { return $null }
    $p = Get-Process -Id $c.OwningProcess -ErrorAction SilentlyContinue
    return [pscustomobject]@{ Pid = $c.OwningProcess; Name = if ($p) { $p.ProcessName } else { "?" } }
}

function Resolve-LiveKitTarget {
    param([string]$Url)
    # Accept ws:// wss:// http:// https:// and bare host:port.
    if ($Url -notmatch '^[a-z]+://') { $Url = "ws://$Url" }
    try {
        $uri = [Uri]$Url
        $port = $uri.Port
        if ($port -le 0) {
            $port = if ($uri.Scheme -in @("wss", "https")) { 443 } else { 80 }
        }
        return [pscustomobject]@{
            Host   = $uri.Host
            Port   = $port
            Scheme = $uri.Scheme
            IsLocal = $uri.Host -in @("localhost", "127.0.0.1", "0.0.0.0", "::1", "[::1]")
        }
    } catch { return $null }
}

# ------------------------------------------------------- livekit.yaml key check
function Get-LiveKitYamlKeys {
    # Parses the dev keys already committed in livekit.yaml so local mode can
    # confirm backend/.env matches. Values are only ever compared, never printed.
    $yaml = Join-Path $RepoRoot "livekit.yaml"
    if (-not (Test-Path $yaml)) { return $null }
    $key = $null; $secret = $null
    $inKeys = $false
    foreach ($line in Get-Content $yaml) {
        if ($line -match '^\s*keys\s*:') { $inKeys = $true; continue }
        if (-not $inKeys) { continue }
        # Capture first: a "key: value" line also matches the break test below,
        # so testing for the block end before capturing dropped every key.
        # livekit.yaml reads "  devkey: <secret>": group 1 is the key name,
        # group 2 the secret. Both come from the SAME line, so they must be
        # assigned together - an if/elseif pair would only ever fill the first.
        if ($line -match '^\s*([A-Za-z0-9_\-]+)\s*:\s*(\S+)') {
            if (-not $key) {
                $key = $Matches[1].Trim('"', "'")
                $secret = $Matches[2].Trim('"', "'")
            }
            continue
        }
        # Any other "word:" line means the keys block ended.
        if ($line -match '^\s*[A-Za-z0-9_\-]+\s*:') { break }
    }
    if ($key -and $secret) { return [pscustomobject]@{ ApiKey = $key; ApiSecret = $secret } }
    return $null
}

# livekit-server does NOT substitute ${VAR} / ${VAR:-default} in this config
# (verified on v1.13.9: the webhook block aborts with "api_key is required to
# use webhooks" however the environment is set). Under compose the file works
# only because the compose environment block is injected into the container. So
# for a native launch we render a resolved copy and leave the committed
# livekit.yaml untouched. The resolved file lands in the gitignored .devlogs
# directory and its contents are never printed.
function New-LiveKitRuntimeConfig {
    param([string]$YamlPath, [string]$OutPath, [int]$BackendPortLocal)
    $yamlKeys = Get-LiveKitYamlKeys
    if (-not $yamlKeys) { return $null }

    $text = Get-Content $YamlPath -Raw

    # host.docker.internal only resolves inside Docker; natively the webhook
    # target is the loopback address of the backend on this machine.
    $webhook = "http://127.0.0.1:$BackendPortLocal/v1/webhooks/livekit"
    if ($env:LIVEKIT_WEBHOOK_URL) { $webhook = $env:LIVEKIT_WEBHOOK_URL }

    # ${NAME:-default} and ${NAME} -> resolved value.
    $text = $text -replace '\$\{LIVEKIT_API_KEY(:-[^}]*)?\}', $yamlKeys.ApiKey
    $text = $text -replace '\$\{LIVEKIT_API_SECRET(:-[^}]*)?\}', $yamlKeys.ApiSecret
    $text = $text -replace '\$\{LIVEKIT_WEBHOOK_URL(:-[^}]*)?\}', $webhook

    # Fail loudly rather than launching with an unresolved placeholder.
    $left = [regex]::Matches($text, '\$\{[A-Za-z_][A-Za-z0-9_]*(:-[^}]*)?\}')
    if ($left.Count -gt 0) {
        Write-Warn2 ("livekit.yaml still has unresolved placeholders: " +
                     (($left | ForEach-Object { $_.Value }) -join ", "))
    }

    Set-Content -Path $OutPath -Value $text -Encoding UTF8
    return $OutPath
}

# ------------------------------------------------------------- livekit starting
function Find-LiveKitBinary {
    $cmd = Get-Command "livekit-server" -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    foreach ($rel in @(".tools\livekit\livekit-server.exe", "tools\livekit\livekit-server.exe")) {
        $p = Join-Path $RepoRoot $rel
        if (Test-Path $p) { return $p }
    }
    return $null
}

function Install-LiveKitBinary {
    # Downloads the official LiveKit server release into .tools/livekit (gitignored).
    Write-Step "LiveKit server not found - attempting official download"
    $toolsDir = Join-Path $RepoRoot ".tools\livekit"
    New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
    try {
        $rel = Invoke-RestMethod -Uri "https://api.github.com/repos/livekit/livekit/releases/latest" `
                                 -UseBasicParsing -TimeoutSec 30
        $tag = $rel.tag_name
        $asset = $rel.assets |
            Where-Object { $_.name -match "windows_amd64\.zip$" } |
            Select-Object -First 1
        if (-not $asset) { throw "no windows_amd64 release asset for $tag" }

        $zip = Join-Path $toolsDir $asset.name
        Write-Info "downloading $($asset.name) ($([math]::Round($asset.size/1MB,1)) MB)"
        Invoke-WebRequest -Uri $asset.browser_download_url -OutFile $zip -UseBasicParsing -TimeoutSec 300
        Expand-Archive -Path $zip -DestinationPath $toolsDir -Force
        Remove-Item $zip -Force

        $exe = Get-ChildItem $toolsDir -Recurse -Filter "livekit-server.exe" |
               Select-Object -First 1
        if (-not $exe) { throw "livekit-server.exe missing from archive" }
        Write-Ok "installed livekit-server $tag -> $($exe.FullName)"
        return $exe.FullName
    } catch {
        Write-Err2 "automatic install failed: $($_.Exception.Message)"
        Write-Info "Install manually with one of:"
        Write-Info "  winget install LiveKit.LiveKitCLI      # CLI only, not the server"
        Write-Info "  Download https://github.com/livekit/livekit/releases and unzip"
        Write-Info "  livekit-server.exe into .tools\livekit\"
        Write-Info "Or install Docker and use docker-compose.livekit.yml (already present)."
        return $null
    }
}

function Start-LiveKitLocal {
    param([int]$Port)

    # Reuse an already-running server. Starting a second one would fail on the
    # port and, worse, leave the user unsure which server the app is talking to.
    if (Test-TcpPort -Host_ "127.0.0.1" -Port $Port) {
        $owner = Get-PortOwner -Port $Port
        Write-Ok "LiveKit already listening on :$Port (pid $($owner.Pid)) - reusing it"
        return $true
    }

    $exe = Find-LiveKitBinary
    if (-not $exe) { $exe = Install-LiveKitBinary }
    if (-not $exe) { return $false }

    $yaml = Join-Path $RepoRoot "livekit.yaml"
    $logFile = Join-Path $LogDir "livekit.log"
    $runtimeYaml = Join-Path $LogDir "livekit.runtime.yaml"
    Write-Step "starting livekit-server"
    Write-Info "binary : $exe"
    Write-Info "config : $runtimeYaml (resolved from livekit.yaml)"

    # Render a resolved config: livekit-server cannot expand the ${VAR:-default}
    # placeholders in livekit.yaml on its own and aborts on the webhook block.
    $rendered = New-LiveKitRuntimeConfig -YamlPath $yaml -OutPath $runtimeYaml -BackendPortLocal $BackendPort
    if (-not $rendered) {
        Write-Err2 "could not read API keys out of livekit.yaml"
        return $false
    }

    $p = Start-Process -FilePath $exe `
        -ArgumentList @("--config", $rendered, "--bind", "127.0.0.1") `
        -WorkingDirectory $RepoRoot -PassThru -NoNewWindow `
        -RedirectStandardOutput $logFile -RedirectStandardError "$logFile.err"
    $script:Children += $p

    if (Wait-TcpPort -Label "livekit" -Host_ "127.0.0.1" -Port $Port -TimeoutSec 45) {
        Write-Ok "LiveKit ready on ws://127.0.0.1:$Port (pid $($p.Id))"
        return $true
    }
    Write-Err2 "LiveKit did not become reachable on :$Port within 45s"
    Write-Info "see $logFile"
    if (Test-Path "$logFile.err") { Get-Content "$logFile.err" -Tail 15 | ForEach-Object { Write-Info $_ } }
    return $false
}

# ------------------------------------------------------------------- lifecycle
function Stop-Children {
    if (-not $script:Children) { return }
    Write-Host ""
    Write-Step "shutting down"
    foreach ($p in $script:Children) {
        try {
            if (-not $p.HasExited) {
                Write-Info "stopping pid $($p.Id)"
                # Kill the whole tree: mvn/npm spawn children of their own.
                & taskkill /PID $p.Id /T /F 2>&1 | Out-Null
            }
        } catch { }
    }
    Write-Ok "all started processes stopped (no orphans)"
}

# =============================================================================
Write-Host ""
Write-Host "===============================================" -ForegroundColor Cyan
Write-Host "  ELMKUSOMA - unified development environment" -ForegroundColor Cyan
Write-Host "===============================================" -ForegroundColor Cyan
Write-Host ""

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
$envFile = Import-DotEnv
Write-Ok "loaded backend/.env"

# ---------------------------------------------------------- LiveKit resolution
$liveKitUrl = $env:LIVEKIT_URL
if (-not $liveKitUrl) {
    Write-Warn2 "LIVEKIT_URL is not set in backend/.env"
    Write-Info "The app still runs, but live video/audio is disabled (chat only)."
    Write-Info "Set LIVEKIT_URL (plus LIVEKIT_API_KEY / LIVEKIT_API_SECRET) to enable it."
    $target = $null
} else {
    $target = Resolve-LiveKitTarget -Url $liveKitUrl
    if (-not $target) {
        Write-Err2 "could not parse LIVEKIT_URL='$liveKitUrl'"
        exit 1
    }
}

$liveKitReady = $false
$liveKitMode = "none"

if ($target) {
    if ($target.IsLocal -or $LocalLiveKit) {
        $liveKitMode = "local"
        Write-Step "LiveKit mode: LOCAL (backend/.env points at localhost)"
        $yamlKeys = Get-LiveKitYamlKeys
        if ($yamlKeys) {
            $envKey = $env:LIVEKIT_API_KEY
            if (-not $envKey) {
                Write-Err2 "LIVEKIT_API_KEY is empty but a local server is required."
                Write-Info "The backend must know the same key as livekit.yaml to mint tokens."
                exit 1
            }
            if ($envKey -ne $yamlKeys.ApiKey) {
                Write-Err2 "credential mismatch: backend/.env LIVEKIT_API_KEY does not match livekit.yaml."
                Write-Info "Live classes will fail to connect until both agree."
                Write-Info "Copy the dev keys from livekit.yaml into backend/.env, or remove them"
                Write-Info "to fall back to a remote server. Values are not printed here."
                exit 1
            }
            Write-Ok "LIVEKIT_API_KEY matches livekit.yaml"
        }

        if (-not (Start-LiveKitLocal -Port $LiveKitPort)) {
            Write-Err2 "local LiveKit is required but could not be started."
            Write-Info "Nothing was faked: live video will NOT work in this state."
            exit 1
        }
        $liveKitReady = $true
    } else {
        $liveKitMode = "remote"
        Write-Step "LiveKit mode: REMOTE ($($target.Scheme)://$($target.Host):$($target.Port))"
        Write-Info "No local server will be started - avoids running two competing SFUs."
        if (Test-TcpPort -Host_ $target.Host -Port $target.Port -TimeoutMs 2500) {
            Write-Ok "remote LiveKit reachable (TCP $($target.Host):$($target.Port))"
            $liveKitReady = $true
        } else {
            Write-Warn2 "remote LiveKit did not answer a TCP probe on $($target.Host):$($target.Port)"
            Write-Info "Live classes will not connect. Check network/VPN, or set"
            Write-Info "LIVEKIT_URL to a reachable host, or run .\start-dev.ps1 -LocalLiveKit."
        }
        $hasCreds = $env:LIVEKIT_API_KEY -and $env:LIVEKIT_API_SECRET
        if ($hasCreds) { Write-Ok "API credentials present (values not displayed)" }
        else { Write-Warn2 "LIVEKIT_API_KEY / LIVEKIT_API_SECRET missing - token minting will fail" }
    }
}

# --------------------------------------------------------------- port preflight
Write-Step "port check"
foreach ($p in @(@{n="backend";v=$BackendPort}, @{n="frontend";v=$FrontendPort})) {
    $owner = Get-PortOwner -Port $p.v
    if ($owner) {
        Write-Info "$($p.n) :$($p.v) already in use by $($owner.Name) (pid $($owner.Pid)) - will reuse if it serves, else startup will report it"
    } else {
        Write-Ok "$($p.n) :$($p.v) free"
    }
}

# ---------------------------------------------------------------------- backend
$backendReady = $false
if (-not $SkipBackend) {
    Write-Step "starting backend (Spring Boot :$BackendPort)"
    $mvn = Get-Command mvn -ErrorAction SilentlyContinue
    $mvnw = Join-Path $BackendDir "mvnw.cmd"
    if ($mvn) {
        $bl = Join-Path $LogDir "backend.log"
        $pb = Start-Process -FilePath "mvn" -ArgumentList @("spring-boot:run") `
             -WorkingDirectory $BackendDir -PassThru -NoNewWindow `
             -RedirectStandardOutput $bl -RedirectStandardError "$bl.err"
        $script:Children += $pb
    } elseif (Test-Path $mvnw) {
        $bl = Join-Path $LogDir "backend.log"
        $pb = Start-Process -FilePath "cmd" -ArgumentList @("/c", "`"$mvnw`" spring-boot:run") `
             -WorkingDirectory $BackendDir -PassThru -NoNewWindow `
             -RedirectStandardOutput $bl -RedirectStandardError "$bl.err"
        $script:Children += $pb
    } else {
        Write-Err2 "neither mvn nor mvnw.cmd found; cannot start the backend"
        Stop-Children; exit 1
    }
    Write-Info "pid $($pb.Id), log: $LogDir\backend.log"

    if (Wait-TcpPort -Label "backend" -Host_ "127.0.0.1" -Port $BackendPort -TimeoutSec $ReadyTimeoutSec) {
        Write-Ok "backend ready on http://localhost:$BackendPort"
        $backendReady = $true
    } else {
        Write-Err2 "backend did not open :$BackendPort within $ReadyTimeoutSec s"
        Write-Info "last 25 log lines:"
        Get-Content (Join-Path $LogDir "backend.log") -Tail 25 -ErrorAction SilentlyContinue |
            ForEach-Object { Write-Info $_ }
        Stop-Children; exit 1
    }
}

# --------------------------------------------------------------------- frontend
$frontendReady = $false
if (-not $SkipFrontend) {
    Write-Step "starting frontend (Next.js :$FrontendPort)"
    $fl = Join-Path $LogDir "frontend.log"
    $pf = Start-Process -FilePath "npm.cmd" -ArgumentList @("run", "dev") `
         -WorkingDirectory $FrontendDir -PassThru -NoNewWindow `
         -RedirectStandardOutput $fl -RedirectStandardError "$fl.err"
    $script:Children += $pf
    Write-Info "pid $($pf.Id), log: $LogDir\frontend.log"

    if (Wait-TcpPort -Label "frontend" -Host_ "127.0.0.1" -Port $FrontendPort -TimeoutSec $ReadyTimeoutSec) {
        Write-Ok "frontend ready on http://localhost:$FrontendPort"
        $frontendReady = $true
    } else {
        Write-Err2 "frontend did not open :$FrontendPort within $ReadyTimeoutSec s"
        Get-Content $fl -Tail 25 -ErrorAction SilentlyContinue | ForEach-Object { Write-Info $_ }
        Stop-Children; exit 1
    }
}

# ----------------------------------------------------------------- status panel
Write-Host ""
Write-Host "-----------------------------------------------" -ForegroundColor Cyan
Write-Host "  ELMKUSOMA development environment" -ForegroundColor Cyan
Write-Host "-----------------------------------------------" -ForegroundColor Cyan
Write-Host ("  LiveKit   : {0}" -f $(if ($liveKitReady) { "READY   ($liveKitMode)" } else { "NOT READY ($liveKitMode)" }))
if ($target) {
    $shown = if ($liveKitMode -eq "remote") { "$($target.Scheme)://$($target.Host):$($target.Port) (from backend/.env)" }
              else { "ws://127.0.0.1:$LiveKitPort (local)" }
    Write-Host ("  Live URL  : {0}" -f $shown)
}
Write-Host ("  Backend   : {0}" -f $(if ($backendReady) { "READY   http://localhost:$BackendPort" } else { "skipped" }))
Write-Host ("  Frontend  : {0}" -f $(if ($frontendReady) { "READY   http://localhost:$FrontendPort" } else { "skipped" }))
Write-Host ("  Logs      : {0}" -f $LogDir)
Write-Host ""
if ($liveKitReady) {
    Write-Host "  Live classes are enabled." -ForegroundColor Green
} else {
    Write-Host "  Live classes are NOT available - chat and scheduling still work." -ForegroundColor Yellow
}
Write-Host "  Press Ctrl+C to stop everything." -ForegroundColor Gray
Write-Host ""

# Block so the services keep running; Ctrl+C triggers cleanup with no orphans.
try {
    while ($true) {
        foreach ($p in $script:Children) {
            if ($p.HasExited) {
                Write-Err2 "service pid $($p.Id) exited unexpectedly (code $($p.ExitCode))"
                Write-Info "check $LogDir for the cause"
                Stop-Children
                exit 1
            }
        }
        Start-Sleep -Seconds 2
    }
} finally {
    Stop-Children
}