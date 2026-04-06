package com.example.store.step04.workflow;

import com.example.store.step04.model.MerchItem;
import com.example.store.step04.model.Order;
import io.dapr.client.DaprClient;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FetchItemsActivity implements WorkflowActivity {

    private static final Logger log = LoggerFactory.getLogger(FetchItemsActivity.class);

    private final DaprClient daprClient;

    public FetchItemsActivity(DaprClient daprClient) {
        this.daprClient = daprClient;
    }

    @Override
    public Object run(WorkflowActivityContext ctx) {
        Order order = ctx.getInput(Order.class);
        log.info("Fetching items from warehouse for order: {}", order.orderId());

//        // Call the warehouse service via Dapr service invocation
//        List items = daprClient.invokeMethod(
//                "warehouse",
//                "items/" + order.orderId(),
//                null,
//                io.dapr.client.domain.HttpExtension.GET,
//                List.class
//        ).block();

        List<MerchItem> items = new ArrayList<>();
        items.add(new MerchItem("spring boot", "socks", 10, 1.0, "" ));
        items.add(new MerchItem("spring boot", "t-shirt", 10, 1.0, ""));
        items.add(new MerchItem("spring boot", "hoodie", 10, 1.0, ""));

        log.info("Fetched {} items from warehouse for order: {}", items != null ? items.size() : 0, order.orderId());
        return items;
    }
}
