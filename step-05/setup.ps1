#Requires -Version 5.1
<#
.SYNOPSIS
  Installs kind (Kubernetes in Docker) and Dapr 1.17 via Helm.
.DESCRIPTION
  - Checks whether kind is installed; downloads and installs it if not.
  - Installs Dapr 1.17.0 using Helm into the dapr-system namespace.
  Run with: powershell -ExecutionPolicy Bypass -File setup.ps1
#>

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Write-Info  { param($msg) Write-Host "[INFO]  $msg" -ForegroundColor Cyan }
function Write-Warn  { param($msg) Write-Host "[WARN]  $msg" -ForegroundColor Yellow }
function Write-Err   { param($msg) Write-Host "[ERROR] $msg" -ForegroundColor Red; exit 1 }

# ─── Detect arch ─────────────────────────────────────────────────────────────
$arch = if ($env:PROCESSOR_ARCHITECTURE -eq "AMD64" -or $env:PROCESSOR_ARCHITEW6432 -eq "AMD64") { "amd64" } else { "arm64" }

# ─── Install kind if missing ─────────────────────────────────────────────────
$kindCmd = Get-Command kind -ErrorAction SilentlyContinue

if ($kindCmd) {
    Write-Info "kind already installed: $(kind version)"
} else {
    Write-Info "kind not found — installing..."

    $kindVersion = "v0.27.0"
    $kindUrl     = "https://kind.sigs.k8s.io/dl/$kindVersion/kind-windows-$arch.exe"
    $kindDest    = "$env:LOCALAPPDATA\Microsoft\WindowsApps\kind.exe"

    # Prefer a directory already on PATH; fall back to a user-writable location
    $pathDirs = $env:PATH -split ";"
    $writableDir = $pathDirs | Where-Object { $_ -and (Test-Path $_) -and (Test-Path "$_\." -IsValid) } |
                   Where-Object { try { [System.IO.File]::Create("$_\.kindtest").Close(); Remove-Item "$_\.kindtest"; $true } catch { $false } } |
                   Select-Object -First 1

    if (-not $writableDir) {
        $writableDir = "$env:USERPROFILE\.local\bin"
        New-Item -ItemType Directory -Force -Path $writableDir | Out-Null
        # Add to user PATH for this session and persistently
        $env:PATH = "$writableDir;$env:PATH"
        [System.Environment]::SetEnvironmentVariable(
            "PATH",
            "$writableDir;" + [System.Environment]::GetEnvironmentVariable("PATH", "User"),
            "User"
        )
        Write-Warn "Added $writableDir to user PATH. Restart your shell if kind is not found later."
    }

    $kindDest = Join-Path $writableDir "kind.exe"
    Write-Info "Downloading kind from $kindUrl ..."
    Invoke-WebRequest -Uri $kindUrl -OutFile $kindDest -UseBasicParsing
    Write-Info "kind installed to $kindDest"
    Write-Info "kind version: $(& $kindDest version)"
}

# ─── Verify kubectl ───────────────────────────────────────────────────────────
if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) {
    Write-Warn "kubectl not found. kind needs kubectl to interact with the cluster."
    Write-Warn "Install kubectl: https://kubernetes.io/docs/tasks/tools/install-kubectl-windows/"
}

# ─── Verify helm ─────────────────────────────────────────────────────────────
if (-not (Get-Command helm -ErrorAction SilentlyContinue)) {
    Write-Err "helm not found. Install Helm first: https://helm.sh/docs/intro/install/"
}
Write-Info "helm found: $(helm version --short)"

# ─── Install Dapr 1.17 via Helm ──────────────────────────────────────────────
$daprVersion   = "1.17.0"
$daprNamespace = "dapr-system"

Write-Info "Adding / updating Dapr Helm repo..."
helm repo add dapr https://dapr.github.io/helm-charts/ 2>$null
helm repo update dapr

Write-Info "Installing Dapr $daprVersion in namespace '$daprNamespace'..."
helm upgrade --install dapr dapr/dapr `
    --version $daprVersion `
    --namespace $daprNamespace `
    --create-namespace `
    --wait `
    --timeout 5m

Write-Info "Dapr $daprVersion installed successfully."
helm list -n $daprNamespace
