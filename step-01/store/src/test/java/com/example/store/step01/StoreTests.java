package com.example.store.step01;

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
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StoreTests {

    @LocalServerPort
    protected Integer port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Test
    void testSpringAIChatMockTemplate()  {
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

        // Assert that the response contains the expected product information.
        // This is coming from the Microcks mocks response.
        assertNotNull(body);
        assertTrue(body.contains("**Spring Boot**- T-Shirt: 50 units @ $29.99"));
        assertTrue(body.contains("Socks: 100 units @ $12.99"));
        assertTrue(body.contains("Sticker: 200 units @ $4.99"));
        assertTrue(body.contains("**Spring AI**- T-Shirt: 30 units @ $29.99"));
    }
}
