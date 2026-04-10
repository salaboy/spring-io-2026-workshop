package com.example.store.step02;

import io.github.microcks.testcontainers.MicrocksContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureTestRestTemplate
@SpringBootTest(classes = {TestStoreApplication.class, ContainersConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"microcks.enabled=true"})
public class StoreTests {

    @LocalServerPort
    protected Integer port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected MicrocksContainer microcks;

    @Test
    void testSpringAIChatMockTemplate() throws Exception {
        // Call the /api/chat endpoint with a message asking to list all products in the inventory, and check that the response contains the expected product information
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity = new HttpEntity<>("""
                {
                    "conversationId": "abc",
                    "message": "List all the products in the inventory"
                }
            """, headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/api/chat", requestEntity, String.class);

        String body = response.getBody();

        System.err.println("Response body: " + body);

        // Assert that the response contains the expected product information.
        // This is coming from the Microcks mocks response.
        assertNotNull(body);
        assertTrue(body.contains("Here's our complete Spring Merch inventory! We have **30 products** available"));

        boolean anthropicMockInvoked = microcks.verify("Anthropic API", "0.83.0");
        assertTrue(anthropicMockInvoked, "Anthropic API mock should have been invoked");

        boolean warehouseMockInvoked = microcks.verify("Warehouse API", "1.0.0");
        assertTrue(warehouseMockInvoked, "Warehouse API mock should have been invoked");

        long anthropicInvocationsCount = microcks.getServiceInvocationsCount("Anthropic API", "0.83.0");
        assertEquals(3L, anthropicInvocationsCount, "Anthropic API mock should have been invoked three times");
    }
}
