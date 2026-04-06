package com.example.store.step04;

import com.example.store.step04.model.Event;
import io.dapr.client.domain.CloudEvent;
import io.dapr.spring.messaging.DaprMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventsRestController {

    private final EventWebSocketHandler webSocketHandler;

    private List<Event> events = new ArrayList<>();

    private DaprMessagingTemplate<Event> messagingTemplate;


    public EventsRestController(EventWebSocketHandler webSocketHandler, DaprMessagingTemplate<Event> messagingTemplate) {
        this.webSocketHandler = webSocketHandler;
        this.messagingTemplate = messagingTemplate;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void clearEvents() {
        events.clear();
    }

    @PostMapping("/mock")
    public void publishMockEvent(@RequestBody Event event) {
        System.out.println(">> Publishing mock event: " + event);
        messagingTemplate.send("pubsubTopic", event);
    }

    @PostMapping(consumes = "application/cloudevents+json")
    public void receiveEvents(@RequestBody CloudEvent<Event> event) {
        System.out.println(">> Received CloudEvent via Subscription: " + event.getData());
        if (event.getData() != null) {
            Event shippingEvent = event.getData();
            events.add(shippingEvent);
            System.out.println(">> Shipping Event content: " + shippingEvent);
            webSocketHandler.broadcastEvent(shippingEvent);
        }
    }
}
