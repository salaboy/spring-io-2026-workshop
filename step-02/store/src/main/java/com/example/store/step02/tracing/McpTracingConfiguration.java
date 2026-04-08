package com.example.store.step02.tracing;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import org.springframework.ai.mcp.customizer.McpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Propagates the current W3C trace context (traceparent / tracestate) into every
 * outbound HTTP request made by the MCP client transport.
 *
 * Why this is needed:
 *   spring-ai-starter-mcp-client uses the MCP SDK's HttpClientStreamableHttpTransport,
 *   which is backed by Java's java.net.http.HttpClient — NOT Spring's WebClient.
 *   Spring Boot's OTel auto-configuration only instruments WebClient/RestTemplate, so
 *   the MCP transport headers are left uninstrumented and the trace is broken at the
 *   MCP tool-call boundary.
 *
 * Fix:
 *   Register a McpClientCustomizer<HttpClientStreamableHttpTransport.Builder> bean.
 *   The auto-configuration picks it up and calls it before building each transport,
 *   letting us install a per-request customizer that asks Micrometer's Propagator
 *   to inject the active span into the HttpRequest.Builder as HTTP headers.
 */
@Configuration(proxyBeanMethods = false)
public class McpTracingConfiguration {

    @Bean
    public McpClientCustomizer<HttpClientStreamableHttpTransport.Builder> mcpTracingTransportCustomizer(
            Tracer tracer,
            Propagator propagator) {
        return (id, builder) ->
                builder.httpRequestCustomizer((requestBuilder, clientName, uri, bodyType, ctx) ->
                        propagator.inject(
                                tracer.currentTraceContext().context(),
                                requestBuilder,
                                (b, key, value) -> b.header(key, value)
                        )
                );
    }
}
