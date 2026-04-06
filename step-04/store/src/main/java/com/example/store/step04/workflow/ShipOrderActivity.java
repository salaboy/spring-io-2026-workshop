package com.example.store.step04.workflow;

import com.example.store.step04.model.Order;
import io.dapr.client.DaprClient;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ShipOrderActivity implements WorkflowActivity {

    private static final Logger log = LoggerFactory.getLogger(ShipOrderActivity.class);

    private final DaprClient daprClient;

    public ShipOrderActivity(DaprClient daprClient) {
        this.daprClient = daprClient;
    }

    @Override
    public Object run(WorkflowActivityContext ctx) {
        Order order = ctx.getInput(Order.class);
        log.info("Requesting shipment for order: {}", order.orderId());

//        // Call the shipping service via Dapr service invocation
//        String trackingId = daprClient.invokeMethod(
//                "shipping",
//                "ship",
//                order,
//                io.dapr.client.domain.HttpExtension.POST,
//                String.class
//        ).block();

        String trackingId = "123456789";

        log.info("Shipment requested for order: {}, trackingId: {}", order.orderId(), trackingId);
        return trackingId;
    }
}
