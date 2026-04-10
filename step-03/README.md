# Step 3: PubSub Async Communications + Generated MCP

In this step you add event-driven capabilities to the store using [Dapr](https://dapr.io/) PubSub. When an order is placed, we should now call the 3rd-party `shipping` component responsible of shipments. When shipment is recorded, an event is published to a Kafka topic via Dapr, and clients receive real-time updates over a WebSocket connection.

Because we're building an AI-infused app, we want to integrate with `shipping` using MCP! However, `shipping` is 3rd-party and it proposed a gRPC service interface. To automatically, generate a MCP Server from this, we'll use [Reshapr](https://reshapr.io).

## What you will learn

- Wrapping the third-party `shipping` component as a MCP service
- Registering a new MCP tool into the app for shipment requests 
- Using Dapr PubSub for environment-agnostic async messaging
- Subscribing to domain events and forwarding them to WebSocket clients
- Running Kafka locally via Testcontainers for integration tests
- Testing event-driven architectures without coupling to a specific broker

## Architecture

```
Browser ──► Store (port 8080)
              ├── Spring AI ChatClient → Anthropic Claude
                  └── Anthropic Claude (LLM)
                    └── MCP tools (remote, via HTTP)
                          └── Warehouse MCP Server (port 8087)
                                └── Warehouse REST API (port 8086)
                          └── Shipping MCP Server (port 7777)
                                └── Shipping gRPC API (port 9091)
                                    └── Dapr Sidecar ──► Kafka (PubSub broker)
              ├── Kafka (PubSub broker) ──► Dapr Sidecar
                  └── Dapr Sidecar ──► Events Rest Controller (/api/events)
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


## Running the tests

The test suite starts a Kafka container via Testcontainers and a Dapr test runtime:

```bash
cd step-03/store
export ANTHROPIC_API_KEY=your-key-here
mvn test
```

**What to look for in the code:**
- `ContainersConfig.java:67` — we start a `DaprContainer` that gates the PubSub broker
- `StoreTests.java:39` — we're able to send and to receive messages from the PubSub broker in a very easy way.

## Key source files

| File | Description |
|---|---|
| `EventRestController` | Handles the consumption of PubSub messages pushed by the Dapr side car container |
| `EventWebSocketHandler` | Handles WebSocket connections and pushes order events to clients |
| `WebSocketConfig` | Registers the WebSocket handler at `/ws/events` |
| `Event` | Domain event class published to the Dapr PubSub topic |


## Exercices

### Exercice T1: Run store app with Mocked Events

**Goal:** Learn how Microcks + TestContainers decouples services during development and testing. You will run the store service with mocked events from Microcks instead of the real shipping, then verify the store behaves correctly when receiving events.

**Background:** `ContainersConfig` in store declares a Microcks containers ensemble that is only started when the property `microcks.enabled=true` is set. When Microcks is active, we'll use it to register a new subscription in Dapr. For that, we'll use a microcks-provided Kafka topic that will receive mocked messages, so the store receive mock events instead of real ones. The mock events are driven by the examples in `shipping-asyncapi-1.0.0.yaml` — three Spring Boot products (T-Shirt, Socks, Sticker).

**What to look for in the code:**
- `ContainersConfig.java:44` — the `KafkaContainer` that will be used as the PubSub broker
- `ContainersConfig.java:53-62` — the `MicrocksContainersEnsemble` that will produce mocks events
- `ContainersConfig.java:95-100` — the `Darp` subscription that rewires the application to the microcks-provide topic
- `shipping-asyncapi-examples` and `shipping-asyncapi-1.0.0.yaml` — the mock data driving all events

Start the store with the Dapr sidecar, the Kafka container and Microcks:

```bash
cd step-03/store

mvn spring-boot:test-run
```

Open your browser at [http://localhost:8080](http://localhost:8080). Wait a few seconds and observe real-time WebSocket events in the UI. You should understand now where they come from 😉

Congrats! You just validate your store app can receive PubSub messages and transmit them to the UI!

### Exercice R1: Run the Infrastructure Components

**Goal:** Prepare the infrastructure for runn all the components locally. As we rely on a bunch of components, we made your life easy so that you'll just have to run the store appl in the next steps.

> [!NOTE]
> You may still have a `jaeger` container running as we've asked Testcontainers to reuse previous instances. You can now safely stop it to save a few resources.

```bash
cd step-03/store

docker compose up -d
```

This should produce the following output:

```bash
[+] up 11/11
 ✔ Network step-03_default         Created                                              0.0s
 ✔ Container dapr-placement        Started                                              0.4s
 ✔ Container shipping              Started                                              0.4s
 ✔ Container kafka                 Started                                              0.4s
 ✔ Container reshapr-postgres      Started                                              0.5s
 ✔ Container jaeger                Started                                              0.4s
 ✔ Container dapr-scheduler        Started                                              0.4s
 ✔ Container dapr-shipping         Started                                              0.5s
 ✔ Container dapr-store            Started                                              0.5s
 ✔ Container reshapr-control-plane Healthy                                              5.9s
 ✔ Container reshapr-gateway-01    Started                                              5.9s
```

You can inspect the different containers that are running here. We run the shipping 3rd-party service as a container and we have started the reshapr containers for generating an MCP Server for it.

### Exercice A1: Add a new shipping-mcp MCP Server

**Goal:** Learn how Reshapr can easily generate an MCP Server from an API specification.

**Steps:**

1. **Be sure to have the infrastructure up and running** — this was the previous exercise outcome.

2. Install the Reshapr CLI:

    ```bash
    npm install -g @reshapr/reshapr-cli
    ```

3. Login to the reshapr control-plane (running on port 5555):

   ```bash
   reshapr login -s http://localhost:5555 -u admin -p password
   ````

   You should get the following output:
   ```bash
   ℹ️  Logging in to Reshapr at http://localhost:5555...
   ✅ Login successful!
   ℹ️  Welcome, admin!
   ✅ Configuration saved to /Users/<you>/.reshapr/config   
   ```   

4. Import the `shipping-service.proto` file and expose it as an MCP Server wrapping shipping running on port 9091:

   ```bash
   reshapr import -f ./shipping-mcp/shipping-service.proto --be http://shipping:9091
   ```

   You should get the folowwing output:
   ```bash
   ✅ Import successful!
   ℹ️  Discovered Service springio.workshop.v1.ShippingService with ID: 0Q18650R54AFJ
   ✅ Exposition done!
   ✅ Exposition is now active!
   Exposition ID  : 0Q1JVWGQPZKQ9
   Organization   : reshapr
   Created on     : 2026-04-10T14:40:37.565477
   Service ID     : 0Q18650R54AFJ
   Service Name   : springio.workshop.v1.ShippingService
   Service Version: v1
   Service Type   : GRPC -> http://shipping:9091
   Endpoints      : localhost:7777/mcp/reshapr/springio.workshop.v1.ShippingService/v1
   ```

5. Using a MCP Client you can then test the configured endpoint. You can, for example, run [MCPJam](https://www.mcpjam.com/) for that:

   ```bash
   npx @mcpjam/inspector@latest
   ```

6. **Bonus:** Reshapr can also wrap REST or GraphQL API as MCP Servers. Try to import the `https://raw.githubusercontent.com/open-meteo/open-meteo/refs/heads/main/openapi.yml` file and use `https://api.open-meteo.com` as the backend endpoint.


### Exercice R2: Run the app and E2E test

**Goal:** Start the store application and see it connect the dots!

Start the store:

```bash
cd step-03/store

export ANTHROPIC_API_KEY=your-key-here
mvn spring-boot:run
```

Open your browser at [http://localhost:8080](http://localhost:8080).

Place an order and observe real-time WebSocket events in the UI. 🎉

---

## Next step

Proceed to [Step 4: Durable Executions for Spring AI](../step-04/README.md) to make the order-processing pipeline resilient and long-running using Dapr Workflows.
