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

# ─── Step-03 Docker Compose images ───────────────────────────────────────────
info "=== Step-03 Docker Compose images ==="
STEP03_IMAGES=(
  "jaegertracing/jaeger"
  "quay.io/microcks/microcks-uber:1.13.2-native"
  "quay.io/microcks/microcks-uber-async-minion:1.13.2"
  "apache/kafka"
  "daprio/placement"
  "daprio/scheduler"
  "daprio/daprd:1.17.0"
  "ghcr.io/salaboy/springio-shipping:step-03"
  "library/postgres:17-alpine"
  "registry.reshapr.io/reshapr/reshapr-ctrl:nightly"
  "registry.reshapr.io/reshapr/reshapr-proxy:nightly"
)

for img in "${STEP03_IMAGES[@]}"; do
  info "  Pulling $img"
  if docker pull "$img"; then
    echo "$img" >> "$IMAGES_FILE"
  else
    warn "  Failed to pull $img (skipping)"
  fi
done


# ─── Check Java ──────────────────────────────────────────────────────────────
if ! command -v java &>/dev/null; then
  warn "Java is not installed or not on PATH. Java 21+ is required to build the workshop projects."
else
  java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
  if [ "$java_version" -lt 21 ] 2>/dev/null; then
    warn "Java $java_version detected. Java 21 or greater is required for this workshop."
  else
    info "Java $java_version detected — OK."
  fi
fi

# ─── Check Docker ────────────────────────────────────────────────────────────
if ! command -v docker &>/dev/null; then
  warn "Docker is not installed or not on PATH. Docker is required to pull and run images."
else
  info "Docker is available."
fi



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
