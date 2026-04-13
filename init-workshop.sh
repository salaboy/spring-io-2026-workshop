#!/usr/bin/env bash
set -euo pipefail

# ─── Helpers ─────────────────────────────────────────────────────────────────
info()  { echo "[INFO]  $*"; }
warn()  { echo "[WARN]  $*"; }
error() { echo "[ERROR] $*" >&2; exit 1; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
STEP05_DIR="$SCRIPT_DIR/step-05"
IMAGES_FILE="$STEP05_DIR/downloaded-images.txt"

# ─── Pre-fetch Maven dependencies ────────────────────────────────────────────
info "Pre-fetching Maven dependencies from step-01/store..."
cd "$SCRIPT_DIR/step-01/store"
./mvnw clean install -DskipTests
cd "$SCRIPT_DIR"
info "Maven dependencies fetched successfully."


# ─── Check Docker ────────────────────────────────────────────────────────────
if ! command -v docker &>/dev/null; then
  error "Docker is not installed or not on PATH."
fi
if ! docker info &>/dev/null; then
  error "Docker daemon is not running. Please start Docker Desktop and retry."
fi

# ─── Install helm if missing ─────────────────────────────────────────────────
install_helm() {
  info "helm not found — installing..."
  if command -v curl &>/dev/null; then
    curl -fsSL https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
  elif command -v wget &>/dev/null; then
    wget -qO- https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
  else
    error "Neither curl nor wget is available. Please install one and retry."
  fi
}

if command -v helm &>/dev/null; then
  info "helm already installed: $(helm version --short)"
else
  install_helm
  info "helm installed: $(helm version --short)"
fi

# ─── Add Helm repos ──────────────────────────────────────────────────────────
info "Adding / updating Helm repos..."
helm repo add dapr           https://dapr.github.io/helm-charts/                         2>/dev/null || true
helm repo add jaegertracing  https://jaegertracing.github.io/helm-charts                  2>/dev/null || true
helm repo add open-telemetry https://open-telemetry.github.io/opentelemetry-helm-charts   2>/dev/null || true
helm repo add jetstack       https://charts.jetstack.io                                   2>/dev/null || true
helm repo update

# ─── Helper: extract unique image refs from helm template output ──────────────
# Matches lines like:   "        image: foo/bar:tag"
#                  or:  "        - image: foo/bar:tag"
extract_images() {
  awk '/^[[:space:]]+(-[[:space:]]+)?image:[[:space:]]/{
    gsub(/"/, ""); gsub(/'"'"'/, "");
    gsub(/.*image:[[:space:]]*/, "");
    gsub(/[[:space:]].*/, "");
    if (length($0) > 0) print
  }' | sort -u
}

# ─── Helper: pull images from a helm chart ───────────────────────────────────
pull_chart_images() {
  local release="$1"; shift
  info "Templating chart for: $release"
  local images
  images=$(helm template "$release" "$@" 2>/dev/null | extract_images)
  if [[ -z "$images" ]]; then
    warn "No images found for $release — skipping."
    return
  fi
  while IFS= read -r img; do
    [[ -z "$img" ]] && continue
    info "  Pulling $img"
    if docker pull "$img"; then
      echo "$img" >> "$IMAGES_FILE"
    else
      warn "  Failed to pull $img (skipping)"
    fi
  done <<< "$images"
}

# ─── Reset images list ────────────────────────────────────────────────────────
: > "$IMAGES_FILE"

# ─── Helm chart images ────────────────────────────────────────────────────────
info "=== Dapr 1.17.0 ==="
pull_chart_images dapr dapr/dapr \
  --version 1.17.0 \
  --namespace dapr-system

info "=== Kafka (Bitnami chart 22.1.5) ==="
pull_chart_images kafka oci://registry-1.docker.io/bitnamicharts/kafka \
  --version 22.1.5 \
  --set "provisioning.topics[0].name=shipments" \
  --set "provisioning.topics[0].partitions=1" \
  --set "persistence.size=1Gi" \
  --set "image.repository=bitnamilegacy/kafka"

info "=== PostgreSQL (Bitnami chart 12.5.7) ==="
pull_chart_images postgresql oci://registry-1.docker.io/bitnamicharts/postgresql \
  --version 12.5.7 \
  --set "image.debug=true" \
  --set "primary.initdb.user=postgres" \
  --set "primary.initdb.password=postgres" \
  --set "global.postgresql.auth.postgresPassword=postgres" \
  --set "primary.persistence.size=1Gi" \
  --set "image.repository=bitnamilegacy/postgresql"

info "=== Jaeger (chart 3.4.1) ==="
pull_chart_images jaeger jaegertracing/jaeger \
  --version 3.4.1 \
  -f "$STEP05_DIR/k8s-observability/jaeger-values.yaml"

info "=== OpenTelemetry Collector ==="
pull_chart_images otel-collector open-telemetry/opentelemetry-collector \
  --namespace opentelemetry \
  -f "$STEP05_DIR/k8s-observability/collector-config-jaeger-only.yaml"

info "=== cert-manager ==="
pull_chart_images cert-manager jetstack/cert-manager \
  --namespace cert-manager \
  --set crds.enabled=true

info "=== OpenTelemetry Operator ==="
pull_chart_images opentelemetry-operator open-telemetry/opentelemetry-operator \
  --namespace opentelemetry \
  --set "manager.extraArgs={--enable-go-instrumentation}"

# ─── Application images (from step-05/k8s/ manifests) ────────────────────────
info "=== Application images ==="
APP_IMAGES=(
  "ghcr.io/salaboy/springio-warehouse:step-02"
  "ghcr.io/salaboy/springio-warehouse-mcp:step-02"
  "ghcr.io/salaboy/springio-store:step-04"
  "ghcr.io/salaboy/springio-shipping:step-04"
)

for img in "${APP_IMAGES[@]}"; do
  info "  Pulling $img"
  if docker pull "$img"; then
    echo "$img" >> "$IMAGES_FILE"
  else
    warn "  Failed to pull $img (skipping)"
  fi
done

# ─── Deduplicate the final list ───────────────────────────────────────────────
sort -u "$IMAGES_FILE" -o "$IMAGES_FILE"

info ""
info "Done. $(wc -l < "$IMAGES_FILE" | tr -d ' ') images saved to $IMAGES_FILE"

info "Run step-05/setup.sh to create the kind cluster — it will load these images automatically."
