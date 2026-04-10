package com.example.store.step04;

import com.example.store.step04.model.Event;
import io.dapr.spring.messaging.DaprMessagingTemplate;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.wait.strategy.DaprWait;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {TestStoreApplication.class, ContainersConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class StoreTests {

    @Autowired
    private DaprContainer daprContainer;

    @Autowired
    DaprMessagingTemplate<Event> messagingTemplate;

    @Autowired
    EventsRestController eventsRestController;

    @BeforeEach
    void setUp() {
        DaprWait.forSubscription("pubsub", "shipments").waitUntilReady(daprContainer);
        org.testcontainers.Testcontainers.exposeHostPorts(8080);
        eventsRestController.clearEvents();
    }

    @Test
    void testPubSubWithMessagingTemplate() throws InterruptedException {
        //Wait for Dapr to be ready
        Thread.sleep(2000);

        assertTrue(eventsRestController.getEvents().isEmpty());

        messagingTemplate.send("shipments", new Event("1234","SHIPPING", "Shipping of order 123 is 1km away"));

        await().atMost(Duration.ofSeconds(5))
                .until(eventsRestController.getEvents()::size, equalTo(1));



    }
}
