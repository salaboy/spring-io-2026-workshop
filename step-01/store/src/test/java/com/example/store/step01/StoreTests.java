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
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"microcks.enabled=true"})
public class StoreTests {

    @LocalServerPort
    protected Integer port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Test
    void testSpringAIChatMockTemplate()  {
        // Call the /api/chat endpoint with a message asking to list all products in 
        // the inventory, and check that the response contains the expected product information.
    }
}
