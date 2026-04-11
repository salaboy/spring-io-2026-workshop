# Step 5: Running in Kubernetes

In this step you deploy all workshop services to a local Kubernetes cluster using [kind](https://kind.sigs.k8s.io/) (Kubernetes in Docker), with [Dapr](https://dapr.io/), Kafka, PostgreSQL, and a full observability stack installed via [Helm](https://helm.sh/).

## Prerequisites

- Docker — [docker.com](https://www.docker.com/products/docker-desktop/)
- kubectl — [kubernetes.io/docs/tasks/tools](https://kubernetes.io/docs/tasks/tools/)
- An Anthropic API key exported as `ANTHROPIC_API_KEY`

> `kind` and `helm` will be installed automatically by the setup script if they are not already present.

## Step 1 — Run the setup script

The setup script creates a local kind cluster, installs Dapr, Kafka, PostgreSQL, and the full observability stack (Jaeger + OpenTelemetry Collector + Operator).

**macOS / Linux:**
```bash
export ANTHROPIC_API_KEY=<your-key>
cd step-05
chmod +x setup.sh
./setup.sh
```

**Windows (PowerShell):**
```powershell
$env:ANTHROPIC_API_KEY = "<your-key>"
cd step-05
.\setup.ps1
```

## Step 2 — Deploy the services

Apply all Kubernetes manifests:

```bash
kubectl apply -f k8s/
```

## Step 3 — Access the application

Open **two separate terminals** and run one port-forward in each:

**Terminal 1 — Store application (port 8080):**
```bash
kubectl port-forward svc/store 8080:8080
```
Then open [http://localhost:8080](http://localhost:8080)

**Terminal 2 — Jaeger UI (port 16686):**
```bash
kubectl port-forward svc/jaeger-query 16686:16686
```
Then open [http://localhost:16686](http://localhost:16686)

## Cleaning up

```bash
kind delete cluster --name workshop
```
