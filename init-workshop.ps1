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

# ─── Install helm if missing ──────────────────────────────────────────────────
if (Get-Command helm -ErrorAction SilentlyContinue) {
    Write-Info "helm already installed: $(helm version --short)"
} else {
    Write-Info "helm not found — installing..."
    $helmInstallScript = "$env:TEMP\get-helm.ps1"
    Invoke-WebRequest -Uri "https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3.ps1" `
        -OutFile $helmInstallScript -UseBasicParsing
    & $helmInstallScript
    Remove-Item $helmInstallScript -Force
    if (Get-Command helm -ErrorAction SilentlyContinue) {
        Write-Info "helm installed: $(helm version --short)"
    } else {
        Write-Err "helm installation failed. Please install manually: https://helm.sh/docs/intro/install/"
    }
}

# ─── Add Helm repos ───────────────────────────────────────────────────────────
Write-Info "Adding / updating Helm repos..."
helm repo add dapr           https://dapr.github.io/helm-charts/                         2>$null
helm repo add jaegertracing  https://jaegertracing.github.io/helm-charts                  2>$null
helm repo add open-telemetry https://open-telemetry.github.io/opentelemetry-helm-charts   2>$null
helm repo add jetstack       https://charts.jetstack.io                                   2>$null
helm repo update | Out-Null

# ─── Helper: extract unique image refs from helm template output ──────────────
# Matches:   "        image: foo/bar:tag"
#       or:  "        - image: foo/bar:tag"
function Get-ChartImages {
    param([string]$templateOutput)

    $images = $templateOutput -split "`n" |
        Where-Object { $_ -match '^\s+(-\s+)?image:\s+\S' } |
        ForEach-Object {
            $val = ($_ -replace '.*image:\s*', '').Trim().Trim('"').Trim("'")
            ($val -split '\s+')[0]
        } |
        Sort-Object -Unique |
        Where-Object { $_ -ne '' }

    return $images
}

# ─── Helper: template a chart and pull all its images ─────────────────────────
function Pull-ChartImages {
    param(
        [string]   $ReleaseName,
        [string[]] $HelmArgs
    )

    Write-Info "=== Templating chart for: $ReleaseName ==="
    $output = helm template $ReleaseName @HelmArgs 2>$null
    if ($LASTEXITCODE -ne 0 -or -not $output) {
        Write-Warn "helm template failed or returned no output for $ReleaseName — skipping."
        return
    }

    $images = Get-ChartImages -templateOutput $output
    if (-not $images) {
        Write-Warn "No images found for $ReleaseName — skipping."
        return
    }

    foreach ($img in $images) {
        Write-Info "  Pulling $img"
        docker pull $img
        if ($LASTEXITCODE -eq 0) {
            Add-Content -Path $imagesFile -Value $img
        } else {
            Write-Warn "  Failed to pull $img (skipping)"
        }
    }
}

# ─── Reset images list ────────────────────────────────────────────────────────
Set-Content -Path $imagesFile -Value ""

# ─── Helm chart images ────────────────────────────────────────────────────────
Pull-ChartImages -ReleaseName "dapr" -HelmArgs @(
    "dapr/dapr",
    "--version", "1.17.0",
    "--namespace", "dapr-system"
)

Pull-ChartImages -ReleaseName "kafka" -HelmArgs @(
    "oci://registry-1.docker.io/bitnamicharts/kafka",
    "--version", "22.1.5",
    "--set", "provisioning.topics[0].name=shipments",
    "--set", "provisioning.topics[0].partitions=1",
    "--set", "persistence.size=1Gi",
    "--set", "image.repository=bitnamilegacy/kafka"
)

Pull-ChartImages -ReleaseName "postgresql" -HelmArgs @(
    "oci://registry-1.docker.io/bitnamicharts/postgresql",
    "--version", "12.5.7",
    "--set", "image.debug=true",
    "--set", "primary.initdb.user=postgres",
    "--set", "primary.initdb.password=postgres",
    "--set", "global.postgresql.auth.postgresPassword=postgres",
    "--set", "primary.persistence.size=1Gi",
    "--set", "image.repository=bitnamilegacy/postgresql"
)

Pull-ChartImages -ReleaseName "jaeger" -HelmArgs @(
    "jaegertracing/jaeger",
    "--version", "3.4.1",
    "-f", "$step05Dir\k8s-observability\jaeger-values.yaml"
)

Pull-ChartImages -ReleaseName "otel-collector" -HelmArgs @(
    "open-telemetry/opentelemetry-collector",
    "--namespace", "opentelemetry",
    "-f", "$step05Dir\k8s-observability\collector-config-jaeger-only.yaml"
)

Pull-ChartImages -ReleaseName "cert-manager" -HelmArgs @(
    "jetstack/cert-manager",
    "--namespace", "cert-manager",
    "--set", "crds.enabled=true"
)

Pull-ChartImages -ReleaseName "opentelemetry-operator" -HelmArgs @(
    "open-telemetry/opentelemetry-operator",
    "--namespace", "opentelemetry",
    "--set", "manager.extraArgs={--enable-go-instrumentation}"
)

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

# ─── Pre-fetch Maven dependencies ────────────────────────────────────────────
Write-Info "Pre-fetching Maven dependencies from step-01/store..."
Push-Location (Join-Path $scriptDir "step-01\store")
.\mvnw.cmd clean install -DskipTests
Pop-Location
Write-Info "Maven dependencies fetched successfully."

Write-Info "Run step-05/setup.ps1 to create the kind cluster — it will load these images automatically."
