#Requires -Version 5.1
<#
.SYNOPSIS
  Installs kind, Dapr 1.17, Kafka, and PostgreSQL via Helm.
.DESCRIPTION
  - Checks whether kind is installed; downloads and installs it if not.
  - Creates a kind cluster named 'workshop'.
  - Installs Dapr 1.17.0 using Helm into the dapr-system namespace.
  - Installs Kafka (Bitnami OCI chart) with a 'shipments' topic.
  - Installs PostgreSQL (Bitnami OCI chart) with initdb config map.
  Run with: powershell -ExecutionPolicy Bypass -File setup.ps1
#>

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Write-Info  { param($msg) Write-Host "[INFO]  $msg" -ForegroundColor Cyan }
function Write-Warn  { param($msg) Write-Host "[WARN]  $msg" -ForegroundColor Yellow }
function Write-Err   { param($msg) Write-Host "[ERROR] $msg" -ForegroundColor Red; exit 1 }

# ─── Check required environment variables ────────────────────────────────────
if (-not $env:ANTHROPIC_API_KEY) {
    Write-Err "ANTHROPIC_API_KEY is not set. Please set `$env:ANTHROPIC_API_KEY = '<your-key>' and retry."
}
Write-Info "ANTHROPIC_API_KEY is set."

# ─── Check Docker is available (required by kind) ────────────────────────────
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Err "Docker is not installed or not on PATH. Install Docker Desktop from https://www.docker.com/products/docker-desktop/ and retry."
}
$dockerInfo = docker info 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Err "Docker daemon is not running. Please start Docker Desktop and retry."
}
Write-Info "Docker is available."

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

# ─── Install kubectl if missing ──────────────────────────────────────────────
if (Get-Command kubectl -ErrorAction SilentlyContinue) {
    Write-Info "kubectl already installed: $(kubectl version --client --short 2>$null)"
} else {
    Write-Info "kubectl not found — installing..."

    $kubectlVersion = (Invoke-WebRequest -Uri "https://dl.k8s.io/release/stable.txt" -UseBasicParsing).Content.Trim()
    $kubectlUrl     = "https://dl.k8s.io/release/$kubectlVersion/bin/windows/$arch/kubectl.exe"

    $pathDirs = $env:PATH -split ";"
    $writableDir = $pathDirs | Where-Object { $_ -and (Test-Path $_) } |
                   Where-Object { try { [System.IO.File]::Create("$_\.kubectltest").Close(); Remove-Item "$_\.kubectltest"; $true } catch { $false } } |
                   Select-Object -First 1

    if (-not $writableDir) {
        $writableDir = "$env:USERPROFILE\.local\bin"
        New-Item -ItemType Directory -Force -Path $writableDir | Out-Null
        $env:PATH = "$writableDir;$env:PATH"
        [System.Environment]::SetEnvironmentVariable(
            "PATH",
            "$writableDir;" + [System.Environment]::GetEnvironmentVariable("PATH", "User"),
            "User"
        )
        Write-Warn "Added $writableDir to user PATH. Restart your shell if kubectl is not found later."
    }

    $kubectlDest = Join-Path $writableDir "kubectl.exe"
    Write-Info "Downloading kubectl $kubectlVersion from $kubectlUrl ..."
    Invoke-WebRequest -Uri $kubectlUrl -OutFile $kubectlDest -UseBasicParsing
    Write-Info "kubectl installed to $kubectlDest"
    Write-Info "kubectl version: $(& $kubectlDest version --client --short 2>$null)"
}

# ─── Install helm if missing ─────────────────────────────────────────────────
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

# ─── Create kind cluster ─────────────────────────────────────────────────────
$clusterName = "workshop"

$existingClusters = kind get clusters 2>$null
if ($existingClusters -split "`n" | Where-Object { $_.Trim() -eq $clusterName }) {
    Write-Info "kind cluster '$clusterName' already exists — skipping creation."
} else {
    Write-Info "Creating kind cluster '$clusterName'..."
    kind create cluster --name $clusterName --wait 60s
    Write-Info "kind cluster '$clusterName' created."
}

kubectl cluster-info --context "kind-$clusterName"

# ─── Load pre-pulled images into kind (if init-workshop.ps1 was run first) ────
$imagesFile = Join-Path $PSScriptRoot "downloaded-images.txt"
if ((Test-Path $imagesFile) -and (Get-Item $imagesFile).Length -gt 0) {
    Write-Info "Loading pre-pulled images into kind cluster '$clusterName'..."
    Get-Content $imagesFile | Where-Object { $_.Trim() -ne '' } | ForEach-Object {
        $img = $_.Trim()
        # Normalise: strip docker.io/ prefix so Docker can find the image locally
        $localImg = $img -replace '^docker\.io/', ''
        $inspectLocal = docker image inspect $localImg 2>$null
        $inspectFull  = docker image inspect $img      2>$null
        if ($LASTEXITCODE -ne 0 -and -not $inspectLocal) {
            Write-Warn "  $img not found in local Docker cache — will be pulled at deploy time"
            return
        }
        Write-Info "  Loading $img"
        kind load docker-image $localImg --name $clusterName
        if ($LASTEXITCODE -ne 0) {
            kind load docker-image $img --name $clusterName
        }
        if ($LASTEXITCODE -ne 0) {
            Write-Warn "  Could not load $img — it will be pulled at deploy time"
        }
    }
    Write-Info "Image loading complete."
} else {
    Write-Info "No downloaded-images.txt found — run init-workshop.ps1 first to pre-load images."
}

# ─── Create Anthropic API key secret ─────────────────────────────────────────
Write-Info "Creating anthropic-secret in namespace 'default'..."
kubectl create secret generic anthropic-secret `
    --from-literal=api-key="$env:ANTHROPIC_API_KEY" `
    --dry-run=client -o yaml | kubectl apply -f -
Write-Info "anthropic-secret created successfully."

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

# ─── Install Kafka via Helm (Bitnami) ────────────────────────────────────────
Write-Info "Adding / updating Bitnami Helm repo..."
helm repo add bitnami https://charts.bitnami.com/bitnami 2>$null
helm repo update bitnami

Write-Info "Installing Kafka in namespace 'default'..."
helm upgrade --install kafka oci://registry-1.docker.io/bitnamicharts/kafka --version 22.1.5 `
    --set "provisioning.topics[0].name=shipments" `
    --set "provisioning.topics[0].partitions=1" `
    --set "persistence.size=1Gi" `
    --set "image.repository=bitnamilegacy/kafka" `
    --wait `
    --timeout 5m

Write-Info "Kafka installed successfully."

kubectl apply -f k8s/init-db-cm.yaml

# ─── Install PostgreSQL via Helm (Bitnami) ───────────────────────────────────
Write-Info "Installing PostgreSQL in namespace 'default'..."
helm upgrade --install postgresql oci://registry-1.docker.io/bitnamicharts/postgresql --version 12.5.7 `
    --set "image.debug=true" `
    --set "primary.initdb.user=postgres" `
    --set "primary.initdb.password=postgres" `
    --set "global.postgresql.auth.postgresPassword=postgres" `
    --set "primary.persistence.size=1Gi" `
    --set "primary.initdb.scriptsConfigMap=init-db" `
    --set "image.repository=bitnamilegacy/postgresql" `
    --wait `
    --timeout 5m

Write-Info "PostgreSQL installed successfully."
helm list

# ─── Run observability setup ──────────────────────────────────────────────────
Write-Info "Running observability setup..."
& "$PSScriptRoot\setup-observability.ps1"
