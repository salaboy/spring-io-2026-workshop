package com.example.store.step02;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {TestStoreApplication.class, ContainersConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {"microcks.enabled=true"})
public class StoreTests {


    @Test
    void testSpringAIChatMockTemplate()  {
        //@TODO: add tests to validate that the chatbot is
        // working with canned responses

    }
}
