#!/usr/bin/env bash
set -euo pipefail

# ─── Helpers ─────────────────────────────────────────────────────────────────
info()  { echo "[INFO]  $*"; }
warn()  { echo "[WARN]  $*"; }
error() { echo "[ERROR] $*" >&2; exit 1; }

# ─── Check required environment variables ────────────────────────────────────
if [[ -z "${ANTHROPIC_API_KEY:-}" ]]; then
  error "ANTHROPIC_API_KEY is not set. Please export ANTHROPIC_API_KEY=<your-key> and retry."
fi
info "ANTHROPIC_API_KEY is set."

# ─── Check Docker is available (required by kind) ────────────────────────────
if ! command -v docker &>/dev/null; then
  error "Docker is not installed or not on PATH. Install Docker Desktop from https://www.docker.com/products/docker-desktop/ and retry."
fi
if ! docker info &>/dev/null; then
  error "Docker daemon is not running. Please start Docker Desktop and retry."
fi
info "Docker is available: $(docker version --format '{{.Server.Version}}' 2>/dev/null || echo 'running')"

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

# ─── Install kubectl if missing ──────────────────────────────────────────────
install_kubectl() {
  info "kubectl not found — installing..."
  KUBECTL_VERSION="$(curl -fsSL https://dl.k8s.io/release/stable.txt)"
  KUBECTL_URL="https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/${OS}/${ARCH}/kubectl"
  DEST="/usr/local/bin/kubectl"

  if command -v curl &>/dev/null; then
    curl -fsSL "$KUBECTL_URL" -o /tmp/kubectl
  elif command -v wget &>/dev/null; then
    wget -qO /tmp/kubectl "$KUBECTL_URL"
  else
    error "Neither curl nor wget is available. Please install one and retry."
  fi

  chmod +x /tmp/kubectl

  if mv /tmp/kubectl "$DEST" 2>/dev/null; then
    info "kubectl installed to $DEST"
  elif command -v sudo &>/dev/null; then
    sudo mv /tmp/kubectl "$DEST"
    info "kubectl installed to $DEST (via sudo)"
  else
    mkdir -p "$HOME/.local/bin"
    mv /tmp/kubectl "$HOME/.local/bin/kubectl"
    export PATH="$HOME/.local/bin:$PATH"
    warn "kubectl installed to ~/.local/bin/kubectl — ensure this is on your PATH"
  fi
}

if command -v kubectl &>/dev/null; then
  info "kubectl already installed: $(kubectl version --client --short 2>/dev/null || kubectl version --client)"
else
  install_kubectl
  info "kubectl installed: $(kubectl version --client --short 2>/dev/null || kubectl version --client)"
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

# ─── Create kind cluster ─────────────────────────────────────────────────────
CLUSTER_NAME="workshop"

if kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
  info "kind cluster '${CLUSTER_NAME}' already exists — skipping creation."
else
  info "Creating kind cluster '${CLUSTER_NAME}'..."
  kind create cluster --name "${CLUSTER_NAME}" --wait 60s
  info "kind cluster '${CLUSTER_NAME}' created."
fi

kubectl cluster-info --context "kind-${CLUSTER_NAME}"

# ─── Create Anthropic API key secret ─────────────────────────────────────────
info "Creating anthropic-secret in namespace 'default'..."
kubectl create secret generic anthropic-secret \
  --from-literal=api-key="${ANTHROPIC_API_KEY}" \
  --dry-run=client -o yaml | kubectl apply -f -
info "anthropic-secret created successfully."

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

# ─── Install Kafka via Helm (Bitnami) ────────────────────────────────────────
info "Adding / updating Bitnami Helm repo..."
helm repo add bitnami https://charts.bitnami.com/bitnami 2>/dev/null || true
helm repo update bitnami

info "Installing Kafka in namespace 'default'..."
helm upgrade --install kafka oci://registry-1.docker.io/bitnamicharts/kafka --version 22.1.5 --set "provisioning.topics[0].name=shipments" --set "provisioning.topics[0].partitions=1" --set "persistence.size=1Gi" --set "image.repository=bitnamilegacy/kafka" \
  --wait \
  --timeout 5m

info "Kafka installed successfully."

kubectl apply -f k8s/init-db-cm.yaml

# ─── Install PostgreSQL via Helm (Bitnami) ───────────────────────────────────
info "Installing PostgreSQL in namespace 'default'..."
helm upgrade --install postgresql oci://registry-1.docker.io/bitnamicharts/postgresql --version 12.5.7 --set "image.debug=true" --set "primary.initdb.user=postgres" --set "primary.initdb.password=postgres" --set "global.postgresql.auth.postgresPassword=postgres" --set "primary.persistence.size=1Gi" --set "primary.initdb.scriptsConfigMap=init-db" --set "image.repository=bitnamilegacy/postgresql" \
  --wait \
  --timeout 5m

info "PostgreSQL installed successfully."
helm list

# ─── Run observability setup ──────────────────────────────────────────────────
info "Running observability setup..."
bash "$(dirname "${BASH_SOURCE[0]}")/setup-observability.sh"
