# Repairs the two environment gaps that made the Playwright suite fail for reasons unrelated
# to the application, then provisions the fixtures provider-workspace.spec.ts declares.
#
# Gap 1: otp-verification.spec.ts shells out to `psql` via spawnSync. PostgreSQL's bin directory
#        is not on PATH, so it failed with ENOENT. Not a code defect - the spec's approach is
#        legitimate, the runner was incomplete.
# Gap 2: provider-workspace.spec.ts refers to provider-admin@arushatc-e2e.test and institution
#        11111111-1111-1111-1111-111111111111. Both are constants in the spec; nothing in the
#        repository ever created them. Not a code defect either - a missing fixture.
#
# Idempotent: safe to re-run.

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$frontend = Join-Path $repoRoot "frontend"
$psql = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
$sql = Join-Path $PSScriptRoot "seed-provider-workspace.sql"

if (-not (Test-Path -LiteralPath $psql)) {
    throw "psql not found at $psql. Set PGPATH or install PostgreSQL client tools."
}

# Gap 1 fix: put the PostgreSQL client on PATH for this session so spawnSync can find psql.
$env:Path = "C:\Program Files\PostgreSQL\18\bin;$env:Path"
$env:PGPASSWORD = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "5050" }

Write-Host "Applying E2E fixtures..." -ForegroundColor Cyan
& $psql -U postgres -d elmkusoma -v ON_ERROR_STOP=1 -f $sql
if ($LASTEXITCODE -ne 0) {
    throw "Fixture script failed with exit code $LASTEXITCODE"
}

Write-Host ""
Write-Host "Verifying:" -ForegroundColor Cyan
& $psql -U postgres -d elmkusoma -At -c `
    "SELECT 'provider user  : ' || count(*) FROM users WHERE email = 'provider-admin@arushatc-e2e.test';"
& $psql -U postgres -d elmkusoma -At -c `
    "SELECT 'foreign inst   : ' || count(*) FROM institutions WHERE id = '11111111-1111-1111-1111-111111111111';"
& $psql -U postgres -d elmkusoma -At -c `
    "SELECT 'membership     : ' || count(*) FROM institution_memberships m
       JOIN users u ON u.id = m.user_id WHERE u.email = 'provider-admin@arushatc-e2e.test';"
& $psql -U postgres -d elmkusoma -At -c `
    "SELECT 'provider row   : ' || count(*) FROM nfe_education_providers p
       WHERE p.name = 'E2E Provider Workspace' AND p.is_deleted = false;"

Write-Host ""
Write-Host "Fixtures ready. Run: npx playwright test" -ForegroundColor Green