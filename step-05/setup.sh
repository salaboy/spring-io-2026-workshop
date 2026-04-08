#!/usr/bin/env bash
set -euo pipefail

# ─── Helpers ─────────────────────────────────────────────────────────────────
info()  { echo "[INFO]  $*"; }
warn()  { echo "[WARN]  $*"; }
error() { echo "[ERROR] $*" >&2; exit 1; }

# ─── Detect OS / arch ────────────────────────────────────────────────────────
OS="$(uname -s | tr '[:upper:]' '[:lower:]')"
ARCH="$(uname -m)"
case "$ARCH" in
  x86_64)  ARCH="amd64" ;;
  aarch64|arm64) ARCH="arm64" ;;
  *) error "Unsupported architecture: $ARCH" ;;
esac

# ─── Install kind if missing ──────────────────────────────────────────────────
install_kind() {
  info "kind not found — installing..."
  KIND_VERSION="v0.27.0"
  KIND_URL="https://kind.sigs.k8s.io/dl/${KIND_VERSION}/kind-${OS}-${ARCH}"
  DEST="/usr/local/bin/kind"

  if command -v curl &>/dev/null; then
    curl -fsSL "$KIND_URL" -o /tmp/kind
  elif command -v wget &>/dev/null; then
    wget -qO /tmp/kind "$KIND_URL"
  else
    error "Neither curl nor wget is available. Please install one and retry."
  fi

  chmod +x /tmp/kind

  if mv /tmp/kind "$DEST" 2>/dev/null; then
    info "kind installed to $DEST"
  elif command -v sudo &>/dev/null; then
    sudo mv /tmp/kind "$DEST"
    info "kind installed to $DEST (via sudo)"
  else
    # fall back to ~/.local/bin
    mkdir -p "$HOME/.local/bin"
    mv /tmp/kind "$HOME/.local/bin/kind"
    export PATH="$HOME/.local/bin:$PATH"
    warn "kind installed to ~/.local/bin/kind — ensure this is on your PATH"
  fi
}

if command -v kind &>/dev/null; then
  info "kind already installed: $(kind version)"
else
  install_kind
  info "kind installed: $(kind version)"
fi

# ─── Verify kubectl is available (required by kind) ──────────────────────────
if ! command -v kubectl &>/dev/null; then
  warn "kubectl not found. kind needs kubectl to interact with the cluster."
  warn "Install kubectl: https://kubernetes.io/docs/tasks/tools/"
fi

# ─── Verify helm is available ────────────────────────────────────────────────
if ! command -v helm &>/dev/null; then
  error "helm not found. Install Helm first: https://helm.sh/docs/intro/install/"
fi
info "helm found: $(helm version --short)"

# ─── Install Dapr 1.17 via Helm ──────────────────────────────────────────────
DAPR_VERSION="1.17.0"
DAPR_NAMESPACE="dapr-system"

info "Adding / updating Dapr Helm repo..."
helm repo add dapr https://dapr.github.io/helm-charts/ 2>/dev/null || true
helm repo update dapr

info "Installing Dapr ${DAPR_VERSION} in namespace '${DAPR_NAMESPACE}'..."
helm upgrade --install dapr dapr/dapr \
  --version "${DAPR_VERSION}" \
  --namespace "${DAPR_NAMESPACE}" \
  --create-namespace \
  --wait \
  --timeout 5m

info "Dapr ${DAPR_VERSION} installed successfully."
helm list -n "${DAPR_NAMESPACE}"
