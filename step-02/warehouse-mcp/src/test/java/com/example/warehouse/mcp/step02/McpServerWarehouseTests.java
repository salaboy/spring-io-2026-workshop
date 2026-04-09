package com.example.warehouse.mcp.step02;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.Content;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = {TestMcpServerWarehouseApplication.class, ContainersConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"microcks.enabled=true"})
class McpServerWarehouseTests {

    @LocalServerPort
    protected Integer port;

    @Test
    void testMcpServerTools()  {
        withClient(client -> {
            final var toolsList = client.listTools();
            assertNotNull(toolsList);
            assertNotNull(toolsList.tools());
            assertEquals(3, toolsList.tools().size());
        });
    }

    @Test
    void testMcpServerListInventoryTool()  {
        withClient(client -> {
            final var request = new McpSchema.CallToolRequest("list_inventory", Map.of());
            final var response = client.callTool(request);
            assertNotNull(response);
            List<Content> contents = response.content();
            assertNotNull(contents);
            assertEquals(1, contents.size());
            assertEquals("text", contents.getFirst().type());
            String productsList = ((TextContent) contents.getFirst()).text();

            // Convert the JSON string to a Map.
            Map<String, Object> inventoryMap = new ObjectMapper().readValue(productsList, new TypeReference<>() {});
            assertEquals(1, inventoryMap.size());
            // Extract the "products" list from the map.
            List<?> products = (List<?>) inventoryMap.get("products");
            assertEquals(3, products.size());
        });
    }

    // Helper method to create a McpClient and execute a function with it
    private void withClient(Consumer<McpSyncClient> func) {
        final var transport = HttpClientStreamableHttpTransport
                .builder("http://localhost:" + port + "/mcp")
                .build();
        final var mcpClient = McpClient.sync(transport).build();
        mcpClient.initialize();
        mcpClient.ping();
        func.accept(mcpClient);
        mcpClient.closeGracefully();
    }
}
