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
- An Anthropic API key — [Anthorpic Get Started](https://platform.claude.com/docs/en/get-started)

## Running the application

```bash
cd step-01/store

export ANTHROPIC_API_KEY=your-key-here

mvn spring-boot:run
```

Open your browser at [http://localhost:8080](http://localhost:8080).

## Running in dev (test) mode

The test suite uses Testcontainers to start Jaeger (for OTEL trace collection) and Microcks (for API contract validation). Docker must be running.

```bash
cd step-01/store

mvn spring-boot:test-run
```


If you want to run it without ANTHROPIC_API_KEY you run with Microcks: 

```bash
mvn clean -Dspring-boot.run.jvmArguments="-Dmicrocks.enabled=true" spring-boot:test-run
```

You can prompt for "List all items" and you should be able to see the results. 


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



## Exercise: Add a new `@Tool` and observe it in traces

**What you will learn:** how Spring AI registers tools, how the LLM decides which tool to call based on the description, and how each tool invocation appears as a span in Jaeger.

### 1. Add the tool

Open `ChatController.java` and add this method alongside the existing `@Tool` methods:

```java
@Tool(description = "Find Spring merch items within a price range. " +
      "Use this when the user asks for items under, over, or between specific prices.")
public String findItemsByPrice(double minPrice, double maxPrice) {
    List<MerchItem> matches = INVENTORY.stream()
        .filter(item -> item.price() >= minPrice && item.price() <= maxPrice)
        .toList();

    if (matches.isEmpty()) {
        return "No items found between $%.2f and $%.2f".formatted(minPrice, maxPrice);
    }
    return matches.stream()
        .map(i -> "- %s: $%.2f (%d in stock)".formatted(i.displayName(), i.price(), i.quantity()))
        .collect(Collectors.joining("\n"));
}
```

No other wiring is needed — Spring AI picks up every `@Tool`-annotated method in beans registered as tools via `.defaultTools(chatController)` in `ChatRestController`.

### 2. Try it in the UI

Start the application and open [http://localhost:8080](http://localhost:8080). Ask the assistant:

- *"Show me everything under $15"* — does it pick `findItemsByPrice` instead of `listAllItems`?
- *"What is the cheapest item you have?"* — does it combine tools to answer?
- *"Show me items between $10 and $20"* — does it pass both bounds correctly?

### 3. Observe the tool call in Jaeger

Run the tests to start the Jaeger container, then open [http://localhost:XXXX](http://localhost:XXXX). Select the `store` service and find a recent trace. You should see a child span named `findItemsByPrice` nested inside the main chat span.

The property `spring.ai.tools.observations.include-content=true` (already set in `application.properties`) records the tool's input arguments and return value directly on that span — no extra code needed.

### 4. Experiment with the description

The `description` field is the only signal the LLM uses when deciding whether to call a tool. Try these modifications and observe how tool selection changes:

- Make the description vague: `"Get items"` — does the model still choose this tool for price queries?
- Remove price-related wording entirely — does it fall back to `listAllItems`?
- Add a conflicting instruction: `"Never use this for single-item lookups"` — does the model respect it?

This shows that prompt engineering applies to tools just as much as to system prompts.

---

## Next step

Proceed to [Step 2: MCP Tools and API Integrations](../step-02/README.md) to connect the store to a real warehouse service via the Model Context Protocol.
