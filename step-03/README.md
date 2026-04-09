# Step 3: PubSub Async Communications

In this step you add event-driven capabilities to the store using [Dapr](https://dapr.io/) PubSub. When an order is placed, an event is published to a Kafka topic via Dapr, and clients receive real-time updates over a WebSocket connection.

## What you will learn

- Using Dapr PubSub for environment-agnostic async messaging
- Subscribing to domain events and forwarding them to WebSocket clients
- Running Kafka locally via Testcontainers for integration tests
- Testing event-driven architectures without coupling to a specific broker

## Architecture

```
Browser ──► Store (port 8080)
              ├── Spring AI ChatClient → Anthropic Claude
              ├── Dapr Sidecar ──► Kafka (PubSub broker)
              └── WebSocket endpoint (/ws/events)
                    └── EventWebSocketHandler (real-time push to browser)
```

Dapr acts as a portability layer — you can swap the underlying broker (Kafka, RabbitMQ, Redis Streams, etc.) by changing a Dapr component configuration without touching application code.

## Prerequisites

- Java 21 — [adoptium.net](https://adoptium.net/temurin/releases/?version=21)
- Maven — [maven.apache.org](https://maven.apache.org/download.cgi)
- Node.js v22+ and npm — [nodejs.org](https://nodejs.org/en/download)
- Docker — [docker.com](https://www.docker.com/products/docker-desktop/)
- An Anthropic API key — [console.anthropic.com](https://console.anthropic.com/)


## Running the application

Start the store with the Dapr sidecar:

```bash
cd step-03/store

export ANTHROPIC_API_KEY=your-key-here
mvn spring-boot:test-run
```

Open your browser at [http://localhost:8080](http://localhost:8080). Place an order and observe real-time WebSocket events in the UI.

## Running the tests

The test suite starts a Kafka container via Testcontainers and a Dapr test runtime:

```bash
cd step-03/store
export ANTHROPIC_API_KEY=your-key-here
mvn test
```

## Key source files

| File | Description |
|---|---|
| `EventWebSocketHandler` | Handles WebSocket connections and pushes order events to clients |
| `WebSocketConfig` | Registers the WebSocket handler at `/ws/events` |
| `Event` | Domain event class published to the Dapr PubSub topic |


## Next step

Proceed to [Step 4: Durable Executions for Spring AI](../step-04/README.md) to make the order-processing pipeline resilient and long-running using Dapr Workflows.
