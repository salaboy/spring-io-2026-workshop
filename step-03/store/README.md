# Spring Merch Store — Step 01

A Spring Boot application that exposes an AI-powered chat assistant for a Spring merchandise store. Built with **Spring AI** and the **Anthropic Claude** model, it demonstrates tool calling, in-memory conversation history, and OpenTelemetry observability.

## Architecture

```
Client  ──POST /api/chat──►  ChatRestController
                                    │
                                    ▼
                             Spring AI ChatClient
                             (Anthropic Claude)
                                    │
                              tool calling
                                    │
                                    ▼
                              ChatController
                         (inventory + order tools)
```

- **`ChatRestController`** — REST endpoint at `POST /api/chat`. Maintains per-conversation memory using `MessageWindowChatMemory`.
- **`ChatController`** — Spring component exposing `@Tool`-annotated methods to the LLM:
  - `listAllItems` — lists the full inventory
  - `getItemStock` — checks stock for a specific item
  - `displayMerchImages` — returns visual product card data
  - `placeOrder` — places a confirmed order
- **`OpenTelemetryConfiguration`** — exports JVM metrics and HTTP traces via OTLP.
- **`TraceIdFilter`** — propagates trace context across requests.

## Prerequisites

- Java 21+
- Maven 3.9+
- An Anthropic API key

## Running the Application

```bash
export ANTHROPIC_API_KEY=sk-ant-...
./mvnw spring-boot:run
```

The application starts on port `8080`.

## Chat API

**Endpoint:** `POST /api/chat`

**Request body:**
```json
{
  "conversationId": "<string>",
  "message": "<string>"
}
```

- `conversationId` — an arbitrary string used to scope conversation memory. Use the same value across requests to maintain context.
- `message` — the user's message to the assistant.

**Response body:**
```json
{
  "response": "<string>"
}
```

## curl Examples

### List all available merch

```bash
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"conversationId": "session-1", "message": "What items do you have in stock?"}' | jq .
```

### Check stock for a specific item

```bash
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"conversationId": "session-1", "message": "How many Spring Boot T-Shirts are available?"}' | jq .
```

### Browse items for a specific project

```bash
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"conversationId": "session-1", "message": "Show me all Spring AI merch"}' | jq .
```

### Browse stickers only

```bash
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"conversationId": "session-1", "message": "What stickers do you have?"}' | jq .
```

### Add items to order (multi-turn conversation)

```bash
# Turn 1 — ask about socks
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"conversationId": "order-session", "message": "I want 2 Spring Boot T-Shirts and 3 Spring AI Stickers"}' | jq .

# Turn 2 — confirm the order (same conversationId keeps context)
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"conversationId": "order-session", "message": "Yes, please place the order"}' | jq .
```

### Show current order contents

```bash
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"conversationId": "order-session", "message": "What is in my order so far?"}' | jq .
```

### Start a fresh conversation

Simply use a different `conversationId` — each ID gets its own isolated memory:

```bash
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"conversationId": "new-customer-42", "message": "Hello! What can you help me with?"}' | jq .
```

## Observability

The application is pre-configured for OpenTelemetry:

- **Tracing** — exports traces via OTLP HTTP (`/v1/traces`). All chat client prompts and completions are traced.
- **Metrics** — JVM metrics (CPU, memory, threads, class loading) exported in OTel semantic conventions.
- **Actuator** — all management endpoints are exposed at `/actuator`.

For local development, the test configuration (`ContainersConfig`) spins up a **Jaeger** container automatically via Testcontainers.

## Tech Stack

| Component | Version |
|---|---|
| Spring Boot | 4.0.5 |
| Spring AI | 2.0.0-M4 |
| LLM Provider | Anthropic Claude |
| Java | 21 |
| Observability | OpenTelemetry / Micrometer |
