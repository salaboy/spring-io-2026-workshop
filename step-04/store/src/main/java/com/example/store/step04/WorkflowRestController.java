package com.example.store.step04;

import com.example.store.step04.model.Order;
import com.example.store.step04.workflow.ProcessOrderWorkflow;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowInstanceStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowRestController {

    private final DaprWorkflowClient workflowClient;

    public WorkflowRestController(DaprWorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> startWorkflow(@RequestBody Order order) {
        String instanceId = workflowClient.scheduleNewWorkflow(
                ProcessOrderWorkflow.class,
                order
        );
        return ResponseEntity.ok(Map.of("instanceId", instanceId));
    }

    @GetMapping("/{instanceId}")
    public ResponseEntity<WorkflowInstanceStatus> getWorkflowStatus(@PathVariable String instanceId) {
        WorkflowInstanceStatus status = workflowClient.getInstanceState(instanceId, true);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

    @PostMapping("/{instanceId}/events/{eventName}")
    public ResponseEntity<Void> raiseEvent(
            @PathVariable String instanceId,
            @PathVariable String eventName,
            @RequestBody(required = false) String payload) {
        workflowClient.raiseEvent(instanceId, eventName, payload);
        return ResponseEntity.ok().build();
    }
}
