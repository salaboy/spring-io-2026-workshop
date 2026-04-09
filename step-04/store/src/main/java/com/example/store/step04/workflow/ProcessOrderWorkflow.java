package com.example.store.step04.workflow;

import com.example.store.step04.model.Order;
import io.dapr.spring.workflows.config.annotations.WorkflowMetadata;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@WorkflowMetadata(name = "ProcessOrderWorkflow")
public class ProcessOrderWorkflow implements Workflow {

    private static final Logger log = LoggerFactory.getLogger(ProcessOrderWorkflow.class);

    // External event names
    public static final String FETCH_ITEMS_EVENT = "fetch-items";
    public static final String CONFIRM_ORDER_EVENT = "confirm-order";
    public static final String ITEMS_SHIPPED_EVENT = "items-shipped";

    @Override
    public WorkflowStub create() {
        return ctx -> {
            Order order = ctx.getInput(Order.class);
            log.info("ProcessOrderWorkflow started for order: {}", order.orderId());

            // Step 1: Register the order
            String registrationResult = ctx.callActivity(
                    RegisterOrderActivity.class.getName(),
                    order,
                    String.class
            ).await();
            log.info("Step 1 complete - {}", registrationResult);



            // Step 2: Fetch items from the warehouse service
            Object items = ctx.callActivity(
                    FetchItemsActivity.class.getName(),
                    order,
                    Object.class
            ).await();
            log.info("Step 2 complete - fetched items from warehouse for order: {}", order.orderId());


            // Step 3: Call the shipping service
            String trackingId = ctx.callActivity(
                    ShipOrderActivity.class.getName(),
                    order,
                    String.class
            ).await();
            log.info("Step 3 complete - shipment requested, trackingId: {}", trackingId);

//            // Wait for external event confirming items were shipped
//            log.info("Waiting for '{}' event for order: {}", ITEMS_SHIPPED_EVENT, order.orderId());
//            String shippingConfirmation = ctx.waitForExternalEvent(ITEMS_SHIPPED_EVENT, String.class).await();
//            log.info("'{}' event received for order: {}, confirmation: {}", ITEMS_SHIPPED_EVENT, order.orderId(), shippingConfirmation);

            log.info("ProcessOrderWorkflow completed for order: {}", order.orderId());
            ctx.complete("Order processed successfully!");
        };
    }
}
