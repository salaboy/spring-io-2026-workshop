#Requires -Version 5.1
<#
.SYNOPSIS
  Sets up the observability stack: Jaeger, OpenTelemetry Collector, cert-manager, OpenTelemetry Operator.
.DESCRIPTION
  Run with: powershell -ExecutionPolicy Bypass -File setup-observability.ps1
#>

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$scriptDir        = Split-Path -Parent $MyInvocation.MyCommand.Path
$observabilityDir = Join-Path $scriptDir "k8s-observability"

Write-Host "=== Observability Setup ===" -ForegroundColor Cyan
Write-Host ""

# -------------------------------------------------------
# 1. Install Jaeger
# -------------------------------------------------------
Write-Host "--- Installing Jaeger ---"
helm repo add jaegertracing https://jaegertracing.github.io/helm-charts 2>$null
helm repo update

$jaegerStatus = helm status jaeger 2>$null
if ($jaegerStatus) {
    Write-Host "Jaeger is already installed, skipping."
} else {
    helm install jaeger jaegertracing/jaeger --version 3.4.1 `
        -f "$observabilityDir\jaeger-values.yaml" `
        --wait
}
Write-Host "Jaeger pods:"
kubectl get pods -l app.kubernetes.io/name=jaeger
Write-Host ""

# -------------------------------------------------------
# 2. Create OpenTelemetry namespace and configure Dash0
# -------------------------------------------------------
Write-Host "--- Creating OpenTelemetry namespace ---"
kubectl create namespace opentelemetry --dry-run=client -o yaml | kubectl apply -f -

$collectorValues = ""
if ($env:DASH0_AUTH_TOKEN) {
    $dash0Hostname = if ($env:DASH0_ENDPOINT_OTLP_GRPC_HOSTNAME) { $env:DASH0_ENDPOINT_OTLP_GRPC_HOSTNAME } else { "ingress.eu-west-1.aws.dash0.com" }
    $dash0Port     = if ($env:DASH0_ENDPOINT_OTLP_GRPC_PORT)     { $env:DASH0_ENDPOINT_OTLP_GRPC_PORT }     else { "4317" }
    $dash0Dataset  = if ($env:DASH0_DATASET)                      { $env:DASH0_DATASET }                      else { "salaboy" }

    kubectl create secret generic dash0-secrets `
        --from-literal=dash0-authorization-token="$env:DASH0_AUTH_TOKEN" `
        --from-literal=dash0-grpc-hostname="$dash0Hostname" `
        --from-literal=dash0-grpc-port="$dash0Port" `
        --from-literal=dash0-dataset="$dash0Dataset" `
        --namespace=opentelemetry `
        --dry-run=client -o yaml | kubectl apply -f -
    Write-Host "Dash0 secrets created. Collector will export to both Jaeger and Dash0."
    $collectorValues = "$observabilityDir\collector-config.yaml"
} else {
    Write-Host "DASH0_AUTH_TOKEN not set. Collector will export to Jaeger only."
    $collectorValues = "$observabilityDir\collector-config-jaeger-only.yaml"
}
Write-Host ""

# -------------------------------------------------------
# 3. Install OpenTelemetry Collector
# -------------------------------------------------------
Write-Host "--- Installing OpenTelemetry Collector ---"
helm repo add open-telemetry https://open-telemetry.github.io/opentelemetry-helm-charts 2>$null
helm repo update

$otelStatus = helm status otel-collector -n opentelemetry 2>$null
if ($otelStatus) {
    Write-Host "OpenTelemetry Collector is already installed, upgrading with current config."
    helm upgrade otel-collector open-telemetry/opentelemetry-collector `
        --namespace opentelemetry `
        -f "$collectorValues" `
        --wait
} else {
    helm install otel-collector open-telemetry/opentelemetry-collector `
        --namespace opentelemetry `
        -f "$collectorValues" `
        --wait
}
Write-Host "OpenTelemetry Collector pods:"
kubectl get pods -n opentelemetry -l app.kubernetes.io/name=opentelemetry-collector
Write-Host ""

# -------------------------------------------------------
# 4. Install cert-manager
# -------------------------------------------------------
Write-Host "--- Installing cert-manager ---"
helm repo add jetstack https://charts.jetstack.io --force-update
helm repo update

$certManagerStatus = helm status cert-manager -n cert-manager 2>$null
if ($certManagerStatus) {
    Write-Host "cert-manager is already installed, skipping."
} else {
    helm upgrade --install cert-manager jetstack/cert-manager `
        --namespace cert-manager --create-namespace `
        --set crds.enabled=true `
        --wait
}
Write-Host "cert-manager pods:"
kubectl get pods -n cert-manager
Write-Host ""

# -------------------------------------------------------
# 5. Install OpenTelemetry Operator
# -------------------------------------------------------
Write-Host "--- Installing OpenTelemetry Operator ---"
$operatorStatus = helm status opentelemetry-operator -n opentelemetry 2>$null
if ($operatorStatus) {
    Write-Host "OpenTelemetry Operator is already installed, skipping."
} else {
    helm upgrade --install opentelemetry-operator open-telemetry/opentelemetry-operator `
        --namespace opentelemetry `
        --set "manager.extraArgs={--enable-go-instrumentation}" `
        --wait
}
Write-Host "OpenTelemetry Operator pods:"
kubectl get pods -n opentelemetry -l app.kubernetes.io/name=opentelemetry-operator
Write-Host ""

# -------------------------------------------------------
# 6. Apply OpenTelemetry Instrumentation resource
# -------------------------------------------------------
Write-Host "--- Applying OpenTelemetry Instrumentation ---"
kubectl apply -f "$observabilityDir\instrumentation.yaml"
Write-Host "Instrumentation resource applied."
Write-Host ""

Write-Host "--- Applying Dapr Tracing Configuration ---"
kubectl apply -f "$observabilityDir\dapr-tracing.yaml"
Write-Host "Dapr Tracing Configuration applied."
Write-Host ""

Write-Host "=== Observability setup complete ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "To access Jaeger UI:"
Write-Host "  kubectl port-forward svc/jaeger-query 16686"
Write-Host "Then open http://localhost:16686"
