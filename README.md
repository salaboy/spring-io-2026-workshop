# Spring I/O Workshop 2026

Building Testable, Durable and Observable Spring AI applications

## Introduction

This hands-on workshop walks you through building a production-grade AI-powered store application, progressively adding capabilities across five steps — from a basic Spring AI chatbot to a durable, event-driven, Kubernetes-ready system.

Each step builds on the previous one, introducing new patterns and tools. You can follow the steps in order or jump to any step directly using the provided starting code.

## Getting Started

Start by cloning the repository and entering the project directory:

```bash
git clone https://github.com/salaboy/spring-io-2026-workshop.git
cd spring-io-2026-workshop
```

Then run the `init-workshop` script to validate your local environment. It checks that all required tools are installed and configured correctly.

**macOS/Linux:**
```bash
./init-workshop.sh
```

**Windows (PowerShell):**
```powershell
.\init-workshop.ps1
```

Review the output and fix any reported issues before continuing.

---

## Prerequisites

All steps require the following tools. Install them before starting.

### Java 21

Required for all Spring Boot services.

- **macOS:** `brew install openjdk@21`
- **Linux/Windows:** Download from [Adoptium](https://adoptium.net/temurin/releases/?version=21) or [Oracle](https://www.oracle.com/java/technologies/downloads/#java21)

Verify: `java -version`

### Docker (with Docker Compose)

Used to run infrastructure services (Jaeger, Kafka, PostgreSQL, Dapr sidecars) during tests and local development.

- **All platforms:** Install [Docker Desktop](https://www.docker.com/products/docker-desktop/)

Verify: `docker --version && docker compose version`

### Anthropic API Key

The store application uses [Anthropic Claude](https://www.anthropic.com/claude) as its LLM. You need an API key for all steps.

1. Sign up or log in at [console.anthropic.com](https://console.anthropic.com/)
2. Create an API key in your account settings
3. Export it in your shell: `export ANTHROPIC_API_KEY=your-key-here`

---

## Workshop Steps

### [Step 1: Spring AI Application](./step-01/README.md)

Build a Spring AI-powered store with an Anthropic Claude chatbot, contract testing with Microcks, and distributed tracing with OpenTelemetry/Jaeger.

- Spring AI with Anthropic Claude
- API contract testing with Microcks and Testcontainers
- Distributed tracing with OpenTelemetry and Jaeger

### [Step 2: MCP Tools and API Integrations](./step-02/README.md)

Extend the store with a Model Context Protocol (MCP) server and client, allowing the AI to query a live warehouse inventory service.

- Spring AI MCP Server and MCP Client
- Warehouse REST API integration
- Multi-service distributed tracing

### [Step 3: PubSub Async Communications](./step-03/README.md)

Add event-driven capabilities using Dapr PubSub and expose real-time updates via WebSocket, with Kafka as the message broker.

- Dapr PubSub for environment-agnostic async messaging
- Apache Kafka via Testcontainers
- WebSocket for real-time frontend updates
- Testing event-driven architectures

### [Step 4: Durable Executions for Spring AI](./step-04/README.md)

Make the AI application durable using Dapr Workflows to orchestrate long-running agentic order-processing pipelines, and introduce a Go-based gRPC shipping microservice.

- Dapr Workflow for durable, resilient agentic execution
- gRPC-based Go shipping microservice
- PostgreSQL for workflow state persistence

### [Step 5: Running in Kubernetes](./step-05/README.md)

Deploy all services to a local Kubernetes cluster using kind, with Dapr installed via Helm.

- Local Kubernetes cluster with kind
- Dapr on Kubernetes via Helm
- Cloud-native deployment patterns
