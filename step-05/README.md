# Step 5: Running in Kubernetes

In this step you deploy all workshop services to a local Kubernetes cluster using [kind](https://kind.sigs.k8s.io/) (Kubernetes in Docker), with [Dapr](https://dapr.io/) installed via [Helm](https://helm.sh/).

## What you will learn

- Creating a local Kubernetes cluster with kind
- Installing Dapr on Kubernetes using Helm
- Understanding how the cloud-native patterns from previous steps (Dapr PubSub, Workflows, gRPC) translate directly to Kubernetes without code changes

## Prerequisites

- Docker — [docker.com](https://www.docker.com/products/docker-desktop/) (kind runs Kubernetes nodes as Docker containers)
- kubectl — [kubernetes.io/docs/tasks/tools](https://kubernetes.io/docs/tasks/tools/)
- kind — [kind.sigs.k8s.io](https://kind.sigs.k8s.io/#installation)
- Helm 3 — [helm.sh/docs/intro/install](https://helm.sh/docs/intro/install/)

### Installing kubectl

**macOS:**
```bash
brew install kubectl
```

**Linux:**
```bash
curl -LO "https://dl.k8s.io/release/$(curl -sL https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl && sudo mv kubectl /usr/local/bin/
```

**Windows:** Download from [kubernetes.io/docs/tasks/tools/install-kubectl-windows](https://kubernetes.io/docs/tasks/tools/install-kubectl-windows/)

Verify: `kubectl version --client`

### Installing kind

**macOS:**
```bash
brew install kind
```

**Linux / Windows:** Download the binary from [github.com/kubernetes-sigs/kind/releases](https://github.com/kubernetes-sigs/kind/releases) and place it on your `PATH`.

Verify: `kind version`

### Installing Helm

**macOS:**
```bash
brew install helm
```

**Linux:**
```bash
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

**Windows:** Download from [github.com/helm/helm/releases](https://github.com/helm/helm/releases)

Verify: `helm version`

## Setting up the cluster

The `setup.sh` script (or `setup.ps1` on Windows) automates cluster creation and Dapr installation:

```bash
cd step-05
chmod +x setup.sh
./setup.sh
```

**Windows:**
```powershell
cd step-05
.\setup.ps1
```

The script will:
1. Install `kind` if not already present
2. Verify `kubectl` and `helm` are available
3. Add the Dapr Helm chart repository
4. Install **Dapr 1.17.0** into the `dapr-system` namespace

After the script completes, verify Dapr is running:

```bash
kubectl get pods -n dapr-system
```

You should see pods for the Dapr operator, sidecar injector, placement, and scheduler.

## Why this works without code changes

All services were built from the start using cloud-native abstractions:

| Capability | Tool | Kubernetes-ready because... |
|---|---|---|
| PubSub | Dapr | Dapr component swapped at deploy time, no code changes |
| Workflows | Dapr | Dapr sidecar injected automatically by Kubernetes |
| gRPC | standard | Works the same in any environment |
| Observability | OpenTelemetry | Collector endpoint is just a config value |
| Config | Spring / env vars | All secrets/config supplied via Kubernetes Secrets/ConfigMaps |

## Deploying the services

After the cluster is ready, build container images and apply Kubernetes manifests for each service (Store, Warehouse, Warehouse MCP, Shipping). Refer to each service's `Dockerfile` and the deployment manifests in this directory.

```bash
# Point Docker to the kind cluster's registry
kind load docker-image <image-name>:<tag>

# Apply manifests
kubectl apply -f ./manifests/
```

## Cleaning up

To delete the kind cluster when you are done:

```bash
kind delete cluster
```
