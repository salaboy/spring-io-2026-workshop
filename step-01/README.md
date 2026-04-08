# Step 1: Spring AI Application

In this step you build a Spring AI-powered store application with a conversational interface backed by Anthropic Claude. You will add API contract testing with Microcks and distributed tracing with OpenTelemetry and Jaeger.

## What you will learn

- Setting up Spring AI with Anthropic Claude as the LLM provider
- Defining AI tools (`@Tool`) for inventory lookup and order placement
- Testing API contracts with [Microcks](https://microcks.io/) and [Testcontainers](https://testcontainers.com/)
- Instrumenting a Spring Boot application with OpenTelemetry and exporting traces to Jaeger

## Architecture

```
Browser ──► Store (port 8080)
              └── Spring AI ChatClient
                    └── Anthropic Claude (LLM)
                    └── @Tool methods
                          ├── listAllItems()
                          ├── getItemStock()
                          ├── displayMerchImages()
                          └── placeOrder()
```

Traces are exported via OTLP to a local Jaeger instance spun up automatically by Testcontainers during tests.

## Prerequisites

- Java 21 — [adoptium.net](https://adoptium.net/temurin/releases/?version=21)
- Maven — [maven.apache.org](https://maven.apache.org/download.cgi)
- Docker — [docker.com](https://www.docker.com/products/docker-desktop/) (required by Testcontainers)
- An Anthropic API key — [console.anthropic.com](https://console.anthropic.com/)

## Running the application

```bash
cd step-01/store

export ANTHROPIC_API_KEY=your-key-here

mvn spring-boot:run
```

Open your browser at [http://localhost:8080](http://localhost:8080).

## Running the tests

The test suite uses Testcontainers to start Jaeger (for OTEL trace collection) and Microcks (for API contract validation). Docker must be running.

```bash
cd step-01/store

export ANTHROPIC_API_KEY=your-key-here

mvn test
```

To speed up repeated test runs by reusing containers, enable Testcontainers reuse:

```bash
echo "testcontainers.reuse.enable=true" >> ~/.testcontainers.properties
```

## Key configuration

`src/main/resources/application.properties`:

```properties
# LLM provider
spring.ai.anthropic.api-key=${ANTHROPIC_API_KEY}

# Log prompts and completions (useful for learning)
spring.ai.chat.observations.log-prompt=true
spring.ai.chat.observations.log-completion=true

# OpenTelemetry
otel.propagators=tracecontext,b3
otel.resource.attributes.service.name=store

# Export all actuator endpoints
management.endpoints.web.exposure.include=*
management.tracing.sampling.probability=1.0
```

## Key source files

| File | Description |
|---|---|
| `ChatRestController` | REST endpoint `POST /api/chat` — entry point for chat messages |
| `ChatController` | Defines `@Tool` methods callable by the LLM |
| `OpenTelemetryConfiguration` | Configures OTLP trace and metrics export |
| `TraceIdFilter` | Propagates trace context through HTTP requests |

## Next step

Proceed to [Step 2: MCP Tools and API Integrations](../step-02/README.md) to connect the store to a real warehouse service via the Model Context Protocol.
