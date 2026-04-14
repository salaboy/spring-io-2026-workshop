#Requires -Version 5.1
<#
.SYNOPSIS
  Pre-pulls all Docker images needed by the workshop kind cluster.
.DESCRIPTION
  - Installs helm if missing.
  - Uses helm template to discover images from each chart (same flags as step-05/setup.ps1).
  - Pulls all application images from step-05/k8s/ manifests.
  - Saves the full image list to step-05/downloaded-images.txt for setup.ps1 to load into kind.
  Run with: powershell -ExecutionPolicy Bypass -File init-workshop.ps1
#>

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Write-Info { param($msg) Write-Host "[INFO]  $msg" -ForegroundColor Cyan }
function Write-Warn { param($msg) Write-Host "[WARN]  $msg" -ForegroundColor Yellow }
function Write-Err  { param($msg) Write-Host "[ERROR] $msg" -ForegroundColor Red; exit 1 }

$scriptDir  = Split-Path -Parent $MyInvocation.MyCommand.Path
$step05Dir  = Join-Path $scriptDir "step-05"
$imagesFile = Join-Path $step05Dir "downloaded-images.txt"

# ─── Pre-fetch Maven dependencies ────────────────────────────────────────────
Write-Info "Pre-fetching Maven dependencies from step-01/store..."
Push-Location (Join-Path $scriptDir "step-01\store")
.\mvnw.cmd clean install -DskipTests
Pop-Location
Write-Info "Maven dependencies fetched successfully."


# ─── Step-03 Docker Compose images ───────────────────────────────────────────
Write-Info "=== Step-03 Docker Compose images ==="
$step03Images = @(
    "jaegertracing/jaeger",
    "quay.io/microcks/microcks-uber:1.13.2-native",
    "quay.io/microcks/microcks-uber-async-minion:1.13.2",
    "apache/kafka",
    "daprio/placement:1.17.0",
    "daprio/scheduler:1.17.0",
    "daprio/daprd:1.17.0",
    "ghcr.io/salaboy/springio-shipping:step-03",
    "library/postgres:17-alpine",
    "registry.reshapr.io/reshapr/reshapr-ctrl:nightly",
    "registry.reshapr.io/reshapr/reshapr-proxy:nightly"
)

foreach ($img in $step03Images) {
    Write-Info "  Pulling $img"
    docker pull $img
    if ($LASTEXITCODE -eq 0) {
        Add-Content -Path $imagesFile -Value $img
    } else {
        Write-Warn "  Failed to pull $img (skipping)"
    }
}

# ─── Check Java ───────────────────────────────────────────────────────────────
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Warn "Java is not installed or not on PATH. Java 21+ is required to build the workshop projects."
} else {
    $javaVersionOutput = java -version 2>&1 | Select-String 'version'
    if ($javaVersionOutput -match '"(\d+)') {
        $javaMajor = [int]$Matches[1]
        if ($javaMajor -lt 21) {
            Write-Warn "Java $javaMajor detected. Java 21 or greater is required for this workshop."
        } else {
            Write-Info "Java $javaMajor detected — OK."
        }
    } else {
        Write-Warn "Could not determine Java version. Java 21+ is required for this workshop."
    }
}

# ─── Check Docker ─────────────────────────────────────────────────────────────
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Warn "Docker is not installed or not on PATH. Docker is required to pull and run images."
} else {
    Write-Info "Docker is available."
}



# ─── Application images (from step-05/k8s/ manifests) ────────────────────────
Write-Info "=== Application images ==="
$appImages = @(
    "ghcr.io/salaboy/springio-warehouse:step-02",
    "ghcr.io/salaboy/springio-warehouse-mcp:step-02",
    "ghcr.io/salaboy/springio-store:step-04",
    "ghcr.io/salaboy/springio-shipping:step-04"
)

foreach ($img in $appImages) {
    Write-Info "  Pulling $img"
    docker pull $img
    if ($LASTEXITCODE -eq 0) {
        Add-Content -Path $imagesFile -Value $img
    } else {
        Write-Warn "  Failed to pull $img (skipping)"
    }
}

# ─── Deduplicate the final list ───────────────────────────────────────────────
$unique = Get-Content $imagesFile | Where-Object { $_.Trim() -ne '' } | Sort-Object -Unique
Set-Content -Path $imagesFile -Value $unique

$count = ($unique | Measure-Object).Count
Write-Info ""
Write-Info "Done. $count images saved to $imagesFile"

Write-Info "Run step-05/setup.ps1 to create the kind cluster — it will load these images automatically."
