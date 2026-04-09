package com.example.store.step04.workflow;

import com.example.store.step04.model.MerchItem;
import com.example.store.step04.model.Order;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class FetchItemsActivity implements WorkflowActivity {

    private static final Logger log = LoggerFactory.getLogger(FetchItemsActivity.class);

    private final RestClient warehouseRestClient;

    public FetchItemsActivity(RestClient warehouseRestClient) {
        this.warehouseRestClient = warehouseRestClient;
    }

    @Override
    public Object run(WorkflowActivityContext ctx) {
        Order order = ctx.getInput(Order.class);
        log.info("Fetching items from warehouse for order: {}", order.orderId());

        List<MerchItem> items = order.items();
        for (MerchItem item : items) {
            log.info("Acquiring {} x {} {} from warehouse", item.quantity(), item.projectName(), item.type());
            warehouseRestClient.post()
                    .uri("/inventory/{projectName}/{productType}/acquire", item.projectName(), item.type())
                    .body(new AcquireRequest(item.quantity()))
                    .retrieve()
                    .toBodilessEntity();
        }

        log.info("Fetched {} items from warehouse for order: {}", items.size(), order.orderId());
        return items;
    }

    private record AcquireRequest(int quantity) {}
}
