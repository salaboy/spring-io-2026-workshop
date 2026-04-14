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

## Running services in dev mode 

Start the services in this order:

**1. Warehouse REST API (port 8086)**

```bash
cd step-02/warehouse
TESTCONTAINERS_REUSE_ENABLE=true mvn clean -Dspring-boot.run.arguments="--reuse=true" spring-boot:test-run
```

**2. Warehouse MCP Server (port 8087)**

```bash
cd step-02/warehouse-mcp
TESTCONTAINERS_REUSE_ENABLE=true mvn clean -Dspring-boot.run.arguments="--reuse=true" spring-boot:test-run
```

**3. Store (port 8080)**

```bash
cd step-02/store
export ANTHROPIC_API_KEY=your-key-here
TESTCONTAINERS_REUSE_ENABLE=true mvn clean -Dspring-boot.run.arguments="--reuse=true" spring-boot:test-run
```

Open your browser at [http://localhost:8080](http://localhost:8080). The AI can now query live warehouse inventory via MCP.

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

## Exercises

### Exercise T1: Contract-Test warehouse

**Goal:** Learn how Microcks + TestContainers decouples services during development and testing. You will use the OpenAPI contract, to validate that the warehouse component is conformant to the specification. That way, future integration with warehouse-mcp will be smooth.

**Background:** `ContainersConfig` in warehouse declares a Microcks container that is only started when the property `microcks.enabled=true` is set. When Microcks is active, it is loaded with the `warehouse-openapi-1.0.0.yaml` contract that holds the API definition + samples — three Spring Boot products (T-Shirt, Socks, Sticker). Microcks will use these information to automatically create a test suite and acts as a client of the Warehouse API.

**Steps:**

1. Review the `WarehouseAPIContractTests` in `warehouse/src/test/com/example/warehouse/step02`, the `microcks` container is injected into the test class:

   ```java
   @Autowired
   protected MicrocksContainer microcks;
   ```

   We'll then ask `microcks` to launch some tests for us, ensuring the conformance of the warehouse api with the OpenAPI definition. This is done by creating a `TestRequest` and firing the test:

   ```java
   TestRequest testRequest = new TestRequest.Builder()
      .serviceId("Warehouse API:1.0.0")
      .runnerType(TestRunnerType.OPEN_API_SCHEMA.name())
      .testEndpoint("http://host.testcontainers.internal:" + port)
      .build();

   TestResult testResult = microcks.testEndpoint(testRequest);
   ```

   Do you understand where this `host.testcontainers.internal` comes from and whay you need it?

2. Now look at `warehouse/src/test/resources/warehouse-openapi-1.0.0.yaml`. The `examples` blocks under each endpoint define what Microcks will exatrct as sample information.

3. Run warehouse tests with Microcks enabled:

   ```bash
   cd step-02/warehouse
   TESTCONTAINERS_REUSE_ENABLE=true mvn clean \
     -Dspring-boot.run.arguments="--reuse=true" \
     -Dspring-boot.run.jvmArguments="-Dmicrocks.enabled=true" \
     test
   ```

   Look for this line in the output — it confirms Microcks took over as the warehouse backend:
   ```
   [INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.928 s -- in com.example.warehouse.step02.WarehouseAPIContractTests
   ```

4. Explore the detailed traces on console ouput and the assertions within the `WarehouseAPIContractTests` class.

5. **Bonus:** Try adding more assertions in the test so that you can know precisely how many test steps were executed by Microcks when doing the conformance tests. Also, have a look at [Conformance Testing](https://microcks.io/documentation/explanations/conformance-testing/) in Microcks documentation. 

**What to look for in the code:**
- `ContainersConfig.java:68` — the `@ConditionalOnProperty` that gates the Microcks container
- `warehouse-openapi-1.0.0.yaml` — the mock data driving all the test steps

---

### Exercise T2: Run warehouse-mcp with a Mocked Warehouse

**Goal:** Learn how Microcks + TestContainers decouples services during development and testing. You will run the warehouse-mcp service against a Microcks mock instead of the real warehouse, then verify the store behaves correctly using only the mock inventory.

**Background:** `ContainersConfig` in warehouse-mcp declares a Microcks container that is only started when the property `microcks.enabled=true` is set. When Microcks is active, a `DynamicPropertyRegistrar` automatically overrides `application.warehouse-base-url` with the Microcks mock endpoint, so the warehouse-mcp calls the mock instead of the real service. The mock responses are driven by the examples in `warehouse-openapi-1.0.0.yaml` — three Spring Boot products (T-Shirt, Socks, Sticker).

**Steps:**

1. **Do NOT start the warehouse service** — the whole point is to run without it.

2. Start warehouse-mcp in test mode with Microcks enabled:

   ```bash
   cd step-02/warehouse-mcp
   TESTCONTAINERS_REUSE_ENABLE=true mvn clean \
     -Dspring-boot.run.arguments="--reuse=true" \
     -Dspring-boot.run.jvmArguments="-Dmicrocks.enabled=true" \
     spring-boot:test-run
   ```

   Look for this line in the output — it confirms Microcks took over as the warehouse backend:
   ```
   application.warehouse-base-url → http://localhost:<port>/rest/Warehouse API/1.0.0
   ```

3. Start the store (warehouse-mcp must already be running on port 8087):

   ```bash
   cd step-02/store
   export ANTHROPIC_API_KEY=your-key-here
   TESTCONTAINERS_REUSE_ENABLE=true mvn clean \
     -Dspring-boot.run.arguments="--reuse=true" \
     spring-boot:test-run
   ```

4. Open [http://localhost:8080](http://localhost:8080) and ask the chatbot: **"What products do you have in stock?"**

5. **Observe:** The AI will respond with exactly the three products defined in `warehouse-openapi-1.0.0.yaml` (Spring Boot T-Shirt, Spring Boot Socks, Spring Boot Sticker) instead of the full 30-item inventory from the real warehouse service.

6. Now look at `warehouse-mcp/src/test/resources/warehouse-openapi-1.0.0.yaml`. The `examples` blocks under each endpoint define what Microcks will return. Try asking for a specific product — for example **"Tell me about the Spring Boot T-Shirt"** — and verify the price and quantity match the example values.

7. **Bonus:** Try asking the chatbot to place an order for a product. Observe that the acquire call also goes to Microcks (which returns a 200 OK per the spec) and the store completes the order flow end-to-end without any real warehouse.

**What to look for in the code:**
- `ContainersConfig.java:41` — the `@ConditionalOnProperty` that gates the Microcks container
- `ContainersConfig.java:50-57` — the `DynamicPropertyRegistrar` that rewires `application.warehouse-base-url`
- `warehouse-openapi-1.0.0.yaml` — the mock data driving all responses

---

### Exercise A1: Add a New MCP Tool — Filter Inventory by Product Type

**Goal:** Practice the `@McpTool` / `@McpToolParam` annotation pattern and understand how tools are discovered and called by the LLM over the MCP protocol.

**Background:** `WarehouseMcpService` currently exposes three tools: `list_inventory`, `get_product`, and `acquire_product`. You will add a fourth tool, `list_inventory_by_type`, that accepts a product type (e.g. `"T-Shirt"`) and returns only matching products. Because the warehouse REST API has no server-side filtering endpoint, you will implement the filter client-side by reusing `WarehouseClient.getInventory()` and filtering in Java.

**Steps:**

1. Open `WarehouseMcpService.java` (`warehouse-mcp/src/main/java/com/example/warehouse/mcp/step02/WarehouseMcpService.java`).

2. Add the following method:

   ```java
   @McpTool(
       name = "list_inventory_by_type",
       description = "Get all warehouse products of a specific type, e.g. T-Shirt, Socks or Sticker")
   public InventoryResponse getInventoryByType(
           @McpToolParam(description = "Type of the product to filter by") String productType) {
       InventoryResponse all = warehouseClient.getInventory();
       List<ProductResponse> filtered = all.products().stream()
               .filter(p -> p.productType().equalsIgnoreCase(productType))
               .toList();
       return new InventoryResponse(filtered);
   }
   ```

   Add the `java.util.List` import if your IDE does not add it automatically.

3. **Verify the tool is registered.** Open `McpServerWarehouseTests.java` (`warehouse-mcp/src/test/java/com/example/warehouse/mcp/step02/McpServerWarehouseTests.java`). The existing `testMcpServerTools` test asserts exactly `3` tools. Update the assertion to `4`:

   ```java
   assertEquals(4, toolsList.tools().size());
   ```

4. Add a new test method below `testMcpServerListInventoryTool`:

   ```java
   @Test
   void testMcpServerListInventoryByTypeTool() {
       withClient(client -> {
           final var request = new McpSchema.CallToolRequest(
                   "list_inventory_by_type", Map.of("productType", "T-Shirt"));
           final var response = client.callTool(request);
           assertNotNull(response);
           String json = ((TextContent) response.content().getFirst()).text();
           Map<String, Object> result = new ObjectMapper().readValue(json, new TypeReference<>() {});
           List<?> products = (List<?>) result.get("products");
           // All returned products must be T-Shirts
           products.forEach(p -> {
               Map<?, ?> product = (Map<?, ?>) p;
               assertEquals("T-Shirt", product.get("productType"));
           });
       });
   }
   ```

5. Run the tests (Microcks is automatically started by the test class annotation):

   ```bash
   cd step-02/warehouse-mcp
   mvn test
   ```

   Both `testMcpServerTools` and `testMcpServerListInventoryByTypeTool` should pass.

6. Start the full stack (warehouse + warehouse-mcp + store) and ask the chatbot: **"Show me only the T-Shirts you have."** Observe that the LLM now picks `list_inventory_by_type` instead of `list_inventory`, and only T-Shirt products are shown.

**What to look for in the code:**
- `WarehouseMcpService.java` — how `@McpTool` and `@McpToolParam` expose Java methods as LLM-callable tools
- `McpServerWarehouseTests.java:35-39` — how `listTools()` verifies the tool registry at the MCP protocol level
- `McpServerWarehouseTests.java:43-61` — how `callTool()` exercises a tool end-to-end through the MCP transport

---

### Exercise O1: Trace a Full Request Through All Three Services in Jaeger

**Goal:** Understand distributed tracing and how trace context is propagated across service boundaries — including across the MCP HTTP transport that Spring Boot does not instrument automatically.

**Background:** All three services export OpenTelemetry spans to a shared Jaeger instance started by TestContainers. The store adds a `X-Trace-Id` response header (via `TraceIdFilter`) so you can jump directly to the right trace in Jaeger. The MCP client uses `java.net.http.HttpClient`, which Spring Boot's OTel auto-configuration does not instrument — `McpTracingConfiguration` manually injects the `traceparent` header into every MCP request to bridge that gap.

**Steps:**

1. Start all three services in order with container reuse enabled so they share the same Jaeger and Docker network:

   ```bash
   # Terminal 1 — warehouse
   cd step-02/warehouse
   TESTCONTAINERS_REUSE_ENABLE=true mvn clean \
     -Dspring-boot.run.arguments="--reuse=true" spring-boot:test-run

   # Terminal 2 — warehouse-mcp (wait for warehouse to be ready first)
   cd step-02/warehouse-mcp
   TESTCONTAINERS_REUSE_ENABLE=true mvn clean \
     -Dspring-boot.run.arguments="--reuse=true" spring-boot:test-run

   # Terminal 3 — store
   cd step-02/store
   export ANTHROPIC_API_KEY=your-key-here
   TESTCONTAINERS_REUSE_ENABLE=true mvn clean \
     -Dspring-boot.run.arguments="--reuse=true" spring-boot:test-run
   ```

2. Find the Jaeger UI port from the warehouse startup logs. Look for a line like:

   ```
   Container jaegertracing/jaeger is starting...
   ```

   Then check which host port maps to 16686:

   ```bash
   docker ps --format "table {{.Image}}\t{{.Ports}}" | grep jaeger
   ```

   Open `http://localhost:<mapped-port>` in your browser.

3. Send a chat message through the store at [http://localhost:8080](http://localhost:8080) — for example **"What T-Shirts do you have in stock?"** This triggers the full chain: store → warehouse-mcp → warehouse.

4. **Grab the trace ID from the response headers.** Open your browser DevTools (F12 → Network tab), find the `/api/chat/stream` request, and look at the Response Headers for `X-Trace-Id`. Copy that value.

   Alternatively, use curl:
   ```bash
   curl -si -X POST http://localhost:8080/api/chat \
     -H "Content-Type: application/json" \
     -d '{"conversationId":"test-1","message":"What T-Shirts do you have?"}' \
     | grep -i x-trace-id
   ```

5. In the Jaeger UI, paste the trace ID into the search box (top-right "Search by Trace ID") and press Enter. You will see a single trace containing spans from all three services:

   - **store** — the incoming HTTP request span and the Spring AI `ChatClient` span (includes the LLM call and tool dispatch)
   - **warehouse-mcp** — the MCP tool call span (`list_inventory` or similar)
   - **warehouse** — the REST `/inventory` call span

6. Click on the `store → warehouse-mcp` span boundary. Expand its tags and verify that `traceparent` (W3C Trace Context) is present. This header was injected by `McpTracingConfiguration` — without it, the warehouse-mcp span would appear as a completely separate, disconnected trace.

7. **Observe the AI-specific span attributes.** Click on the Spring AI span inside the store service. Look for the `gen_ai.prompt` and `gen_ai.completion` attributes — these are added by `ChatObservationConventionConfig` and contain the full text sent to and received from Anthropic Claude.

8. **Bonus:** Run the same request but with the warehouse service stopped (only warehouse-mcp and store running). Check Jaeger again — you will see the warehouse-mcp span contains an error tag, and you can trace the failure all the way back to the store's response without reading any logs.

**What to look for in the code:**
- `McpTracingConfiguration.java` — why manual trace propagation was needed for the MCP transport and how it injects `traceparent` into HTTP requests
- `TraceIdFilter.java` — how each service extracts the active trace ID and adds it to the response as `X-Trace-Id`
- `ChatObservationConventionConfig.java` — how Spring AI observation conventions are extended to capture prompt and completion text as span attributes

---

## Next step

Proceed to [Step 3: PubSub Async Communications](../step-03/README.md) to add event-driven messaging with Dapr and Kafka.
