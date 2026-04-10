package com.example.store.step04;

import io.dapr.workflows.client.DaprWorkflowClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowRestController {

    private final DaprWorkflowClient daprWorkflowClient;
    public WorkflowRestController(DaprWorkflowClient daprWorkflowClient) {
        this.daprWorkflowClient = daprWorkflowClient;
    }

    @PostMapping("/events")
    public void handleWorkflowEvent(@RequestBody String orderId) {
        System.out.println("Received event for order: " + orderId);
        daprWorkflowClient.raiseEvent(orderId, "confirm-order-delivered", "Confirmed");
    }
}
