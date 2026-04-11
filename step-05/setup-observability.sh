#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OBSERVABILITY_DIR="$SCRIPT_DIR/k8s-observability"

echo "=== Observability Setup ==="
echo ""

# -------------------------------------------------------
# 1. Install Jaeger
# -------------------------------------------------------
echo "--- Installing Jaeger ---"
helm repo add jaegertracing https://jaegertracing.github.io/helm-charts 2>/dev/null || true
helm repo update
if helm status jaeger &>/dev/null; then
  echo "Jaeger is already installed, skipping."
else
  helm install jaeger jaegertracing/jaeger --version 3.4.1 -f "$OBSERVABILITY_DIR/jaeger-values.yaml" --wait
fi
echo "Jaeger pods:"
kubectl get pods -l app.kubernetes.io/name=jaeger
echo ""

# -------------------------------------------------------
# 2. Create OpenTelemetry namespace and configure Dash0
# -------------------------------------------------------
echo "--- Creating OpenTelemetry namespace ---"
kubectl create namespace opentelemetry --dry-run=client -o yaml | kubectl apply -f -

if [ -n "${DASH0_AUTH_TOKEN:-}" ]; then
  DASH0_ENDPOINT_OTLP_GRPC_HOSTNAME="${DASH0_ENDPOINT_OTLP_GRPC_HOSTNAME:-ingress.eu-west-1.aws.dash0.com}"
  DASH0_ENDPOINT_OTLP_GRPC_PORT="${DASH0_ENDPOINT_OTLP_GRPC_PORT:-4317}"
  DASH0_DATASET="${DASH0_DATASET:-salaboy}"

  kubectl create secret generic dash0-secrets \
    --from-literal=dash0-authorization-token="$DASH0_AUTH_TOKEN" \
    --from-literal=dash0-grpc-hostname="$DASH0_ENDPOINT_OTLP_GRPC_HOSTNAME" \
    --from-literal=dash0-grpc-port="$DASH0_ENDPOINT_OTLP_GRPC_PORT" \
    --from-literal=dash0-dataset="$DASH0_DATASET" \
    --namespace=opentelemetry \
    --dry-run=client -o yaml | kubectl apply -f -
  echo "Dash0 secrets created. Collector will export to both Jaeger and Dash0."
  COLLECTOR_VALUES="$OBSERVABILITY_DIR/collector-config.yaml"
else
  echo "DASH0_AUTH_TOKEN not set. Collector will export to Jaeger only."
  COLLECTOR_VALUES="$OBSERVABILITY_DIR/collector-config-jaeger-only.yaml"
fi
echo ""

# -------------------------------------------------------
# 3. Install OpenTelemetry Collector
# -------------------------------------------------------
echo "--- Installing OpenTelemetry Collector ---"
helm repo add open-telemetry https://open-telemetry.github.io/opentelemetry-helm-charts 2>/dev/null || true
helm repo update
if helm status otel-collector -n opentelemetry &>/dev/null; then
  echo "OpenTelemetry Collector is already installed, upgrading with current config."
  helm upgrade otel-collector open-telemetry/opentelemetry-collector \
    --namespace opentelemetry \
    -f "$COLLECTOR_VALUES" \
    --wait
else
  helm install otel-collector open-telemetry/opentelemetry-collector \
    --namespace opentelemetry \
    -f "$COLLECTOR_VALUES" \
    --wait
fi
echo "OpenTelemetry Collector pods:"
kubectl get pods -n opentelemetry -l app.kubernetes.io/name=opentelemetry-collector
echo ""

# -------------------------------------------------------
# 4. Install cert-manager
# -------------------------------------------------------
echo "--- Installing cert-manager ---"
helm repo add jetstack https://charts.jetstack.io --force-update
helm repo update
if helm status cert-manager -n cert-manager &>/dev/null; then
  echo "cert-manager is already installed, skipping."
else
  helm upgrade --install cert-manager jetstack/cert-manager \
    --namespace cert-manager --create-namespace \
    --set crds.enabled=true \
    --wait
fi
echo "cert-manager pods:"
kubectl get pods -n cert-manager
echo ""

# -------------------------------------------------------
# 5. Install OpenTelemetry Operator
# -------------------------------------------------------
echo "--- Installing OpenTelemetry Operator ---"
if helm status opentelemetry-operator -n opentelemetry &>/dev/null; then
  echo "OpenTelemetry Operator is already installed, skipping."
else
  helm upgrade --install opentelemetry-operator open-telemetry/opentelemetry-operator \
    --namespace opentelemetry \
    --set manager.extraArgs='{--enable-go-instrumentation}' \
    --wait
fi
echo "OpenTelemetry Operator pods:"
kubectl get pods -n opentelemetry -l app.kubernetes.io/name=opentelemetry-operator
echo ""

# -------------------------------------------------------
# 6. Apply OpenTelemetry Instrumentation resource
# -------------------------------------------------------
echo "--- Applying OpenTelemetry Instrumentation ---"
kubectl apply -f "$OBSERVABILITY_DIR/instrumentation.yaml"
echo "Instrumentation resource applied."
echo ""

echo "--- Applying Dapr Tracing Configuration ---"
kubectl apply -f "$OBSERVABILITY_DIR/dapr-tracing.yaml"
echo "Dapr Tracing Configuration applied."
echo ""

echo "=== Observability setup complete ==="
echo ""
echo "To access Jaeger UI:"
echo "  kubectl port-forward svc/jaeger-query 16686"
echo "Then open http://localhost:16686"