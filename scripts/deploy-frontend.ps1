# Deploys the frontend to Vercel production.
#
# Why this exists rather than just running `vercel deploy --prod` in frontend/:
#
# This project lives inside OneDrive. Deploying directly from there fails with a
# bare "fetch failed" — six attempts across two days, both upload modes, with the
# payload trimmed to 362 bytes. The deployment gets created but the upload never
# completes, so it sits at UNKNOWN with "Builds: . [0ms]".
#
# Copying the project to a path outside OneDrive first works on the first try.
# The likely cause is OneDrive's on-demand file hydration interfering with the
# reads the CLI does while uploading.
#
# The copy excludes node_modules and .next because Vercel builds on its own
# servers; .vercelignore covers the same ground for anything that slips through.
#
# Usage, from anywhere:
#   powershell -File scripts/deploy-frontend.ps1

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$source = Join-Path $repoRoot "frontend"
$staging = Join-Path $env:TEMP "ak-deploy"

if (-not (Test-Path (Join-Path $source "package.json"))) {
    throw "Could not find frontend/package.json under $repoRoot"
}

Write-Host "Staging outside OneDrive: $staging" -ForegroundColor Cyan
if (Test-Path $staging) {
    [System.IO.Directory]::Delete($staging, $true)
}
New-Item -ItemType Directory -Force -Path $staging | Out-Null

# /E recurses including empty dirs; /XD skips what Vercel rebuilds itself.
robocopy $source $staging /E /XD node_modules .next /NFL /NDL /NJH /NJS /NC /NS | Out-Null

# robocopy uses exit codes below 8 for success with varying detail.
if ($LASTEXITCODE -ge 8) {
    throw "robocopy failed with exit code $LASTEXITCODE"
}

if (-not (Test-Path (Join-Path $staging ".vercel\project.json"))) {
    throw "The .vercel link did not come across; deploying would create a new project. Run 'vercel link' in frontend/ first."
}

$sizeMb = [math]::Round(((Get-ChildItem $staging -Recurse -File | Measure-Object Length -Sum).Sum / 1MB), 2)
Write-Host "Staged $sizeMb MB" -ForegroundColor Cyan

Push-Location $staging
try {
    vercel deploy --prod --yes
}
finally {
    Pop-Location
}
