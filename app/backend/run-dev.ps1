# run-dev.ps1
# Reads .env from the repo root and starts the Spring Boot backend.
# Usage: cd app\backend && .\run-dev.ps1
# Never hardcodes secrets — reads them from .env which is gitignored.

# Find the main worktree root (works from both main repo and git worktrees).
# git rev-parse --git-common-dir returns the common .git dir; its parent is the main repo root.
$gitCommonDir = & git -C $PSScriptRoot rev-parse --git-common-dir 2>$null
if ($gitCommonDir) {
    $repoRoot = Resolve-Path (Join-Path $gitCommonDir "..")
} else {
    $repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..\..")
}
$envFile  = Join-Path $repoRoot ".env"

if (-not (Test-Path $envFile)) {
    Write-Error ".env not found at $envFile. Copy .env.example and fill in your keys."
    exit 1
}

# Parse KEY=VALUE lines (skip comments and blanks)
Get-Content $envFile | Where-Object { $_ -match '^\s*[A-Z_]+=.+' } | ForEach-Object {
    $parts = $_ -split '=', 2
    $key   = $parts[0].Trim()
    $value = $parts[1].Trim()
    [System.Environment]::SetEnvironmentVariable($key, $value, 'Process')
}

Write-Host "Starting Spring Boot backend with env from $envFile ..."
& "$PSScriptRoot\mvnw.cmd" spring-boot:run
