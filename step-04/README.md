# Step 4: Durable Executions for Spring AI

In this step the order-processing pipeline becomes durable and resilient using [Dapr Workflow](https://docs.dapr.io/developing-applications/building-blocks/workflow/). A dedicated Go-based shipping microservice is added, communicating over gRPC, and Docker Compose orchestrates the full multi-service stack.

## What you will learn

- Modelling agentic AI workflows as durable Dapr Workflows with activities
- Surviving process crashes and restarts without losing workflow state (PostgreSQL-backed)
- Building a gRPC microservice in Go and calling it from a Spring Boot application
- Orchestrating a multi-service stack with Docker Compose and Dapr sidecars

## Architecture

```
Browser ──► Store (port 8080)
              ├── Spring AI ChatClient → Anthropic Claude
              ├── WorkflowRestController
              │     └── ProcessOrderWorkflow (Dapr Workflow)
              │           ├── FetchItemsActivity  → Warehouse MCP (port 8087)
              │           ├── RegisterOrderActivity
              │           └── ShipOrderActivity   → Shipping gRPC (port 9091)
              ├── Dapr Sidecar ──► Kafka (PubSub)
              └── WebSocket (/ws/events)

Shipping stack (Docker Compose):
  shipping (Go gRPC, port 9091)
  └── Dapr sidecar (port 50001)
  └── Dapr placement (port 50006)
  └── Dapr scheduler (port 50007)
```

Workflow state is persisted in PostgreSQL so that in-flight workflows survive application restarts.

## Prerequisites

- Java 21 — [adoptium.net](https://adoptium.net/temurin/releases/?version=21)
- Maven — [maven.apache.org](https://maven.apache.org/download.cgi)
- Node.js v22+ and npm — [nodejs.org](https://nodejs.org/en/download)
- Docker with Docker Compose — [docker.com](https://www.docker.com/products/docker-desktop/)
- An Anthropic API key — [console.anthropic.com](https://console.anthropic.com/)
- Go 1.21+ — [go.dev/dl](https://go.dev/dl/)

### Installing Go

**macOS:**
```bash
brew install go
```

**Linux / Windows:** Download from [go.dev/dl](https://go.dev/dl/) and follow the installer.

Verify: `go version`


## Running the shipping service

The shipping microservice runs as a Docker Compose stack:

```bash
cd step-04/shipping
docker compose up --build
```

This starts:
- `shipping` — Go gRPC server (port 9091)
- `dapr` — Dapr sidecar (port 50001)
- `placement` — Dapr placement service (port 50006)
- `scheduler` — Dapr scheduler service (port 50007)

## Running the store

In a separate terminal, also start the warehouse services from Step 2:

```bash
# Terminal 1 — Warehouse REST API
cd step-02/warehouse && mvn spring-boot:run

# Terminal 2 — Warehouse MCP Server
cd step-02/warehouse-mcp && mvn spring-boot:run

# Terminal 3 — Store with Dapr sidecar
cd step-04/store
export ANTHROPIC_API_KEY=your-key-here
mvn spring-boot:run
```

Open your browser at [http://localhost:8080](http://localhost:8080).

## Running the tests

```bash
cd step-04/store
export ANTHROPIC_API_KEY=your-key-here
mvn test
```

Testcontainers starts PostgreSQL and Kafka automatically during the test run.

## Key source files

| Service | File | Description |
|---|---|---|
| store | `ProcessOrderWorkflow` | Dapr Workflow definition orchestrating the order pipeline |
| store | `FetchItemsActivity` | Workflow activity: queries warehouse inventory via MCP |
| store | `RegisterOrderActivity` | Workflow activity: persists the order |
| store | `ShipOrderActivity` | Workflow activity: calls the shipping gRPC service |
| store | `WorkflowRestController` | REST endpoint that triggers the workflow |
| shipping | `main.go` | gRPC server entry point |
| shipping | `server/server.go` | `ShipOrder` RPC implementation |
| shipping | `docker-compose.yml` | Full Dapr + shipping stack |

## Protobuf / gRPC contract

The gRPC interface is defined in [`/shipping-service.proto`](../shipping-service.proto) at the repository root. The Go server implements `ShippingService.ShipOrder`.

## Next step

Proceed to [Step 5: Running in Kubernetes](../step-05/README.md) to deploy all services to a local Kubernetes cluster with Dapr installed via Helm.
