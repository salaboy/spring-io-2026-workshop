package com.example.store.step04.workflow;

import com.example.store.step04.model.Order;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RegisterOrderActivity implements WorkflowActivity {

    private static final Logger log = LoggerFactory.getLogger(RegisterOrderActivity.class);

    @Override
    public Object run(WorkflowActivityContext ctx) {
        Order order = ctx.getInput(Order.class);
        log.info("Registering order: {}", order.orderId());
        // Persist or forward the order to an order management system here
        return "Order " + order.orderId() + " registered";
    }
}
