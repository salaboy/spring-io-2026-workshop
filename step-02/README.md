# Step 2: MCP Tools and API Integrations

In this step you introduce the [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) to connect the store's AI assistant to a live warehouse inventory service. A dedicated MCP server wraps the warehouse REST API and exposes its capabilities as tools that the LLM can call automatically.

## What you will learn

- Running a Spring AI MCP Server that wraps an existing REST API
- Configuring a Spring AI MCP Client in the store to discover and use remote tools
- Propagating OpenTelemetry traces across multiple services
- Testing MCP tool integrations with Microcks API contracts

## Architecture

```
Browser ──► Store (port 8080)
              └── Spring AI ChatClient (MCP Client)
                    └── Anthropic Claude (LLM)
                    └── MCP tools (remote, via HTTP)
                          └── Warehouse MCP Server (port 8087)
                                └── Warehouse REST API (port 8086)
```

All three services emit traces using OpenTelemetry, enabling end-to-end visibility.

## Prerequisites

- Java 21 — [adoptium.net](https://adoptium.net/temurin/releases/?version=21)
- Maven — [maven.apache.org](https://maven.apache.org/download.cgi)
- Node.js v22+ and npm — [nodejs.org](https://nodejs.org/en/download)
- Docker — [docker.com](https://www.docker.com/products/docker-desktop/)
- An Anthropic API key — [console.anthropic.com](https://console.anthropic.com/)

## Running the services

Start the services in this order:

**1. Warehouse REST API (port 8086)**

```bash
cd step-02/warehouse
mvn spring-boot:run
```

**2. Warehouse MCP Server (port 8087)**

```bash
cd step-02/warehouse-mcp
mvn spring-boot:run
```

**3. Store (port 8080)**

```bash
cd step-02/store
export ANTHROPIC_API_KEY=your-key-here
mvn spring-boot:run
```

Open your browser at [http://localhost:8080](http://localhost:8080). The AI can now query live warehouse inventory via MCP.

## Running the tests

Each service has its own test suite using Testcontainers and Microcks. Run them independently:

```bash
cd step-02/warehouse && mvn test
cd step-02/warehouse-mcp && mvn test
cd step-02/store && ANTHROPIC_API_KEY=your-key-here mvn test
```

## Key configuration

**Warehouse MCP Server** (`step-02/warehouse-mcp/src/main/resources/application.properties`):

```properties
server.port=8087
spring.ai.mcp.server.protocol=STREAMABLE
spring.ai.mcp.server.type=SYNC
spring.ai.mcp.server.annotation-scanner.enabled=true

# URL of the warehouse REST API
application.warehouse-base-url=http://localhost:8086
```

**Store MCP Client** — configured to connect to the MCP server at `http://localhost:8087`.

## Key source files

| Service | File | Description |
|---|---|---|
| warehouse | `InventoryController` | REST endpoints for `/inventory` |
| warehouse-mcp | `WarehouseMcpService` | `@Tool`-annotated methods exposed via MCP |
| warehouse-mcp | `WarehouseClient` | RestClient calling the warehouse REST API |
| store | `ChatController` | MCP Client wired into the ChatClient |

## Next step

Proceed to [Step 3: PubSub Async Communications](../step-03/README.md) to add event-driven messaging with Dapr and Kafka.
