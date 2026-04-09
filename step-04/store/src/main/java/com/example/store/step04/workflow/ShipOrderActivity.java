package com.example.store.step04.workflow;

import com.example.store.step04.model.Order;
import com.example.store.step04.shipping.ShipOrderRequest;
import com.example.store.step04.shipping.ShipOrderResponse;
import com.example.store.step04.shipping.ShippingServiceGrpc;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ShipOrderActivity implements WorkflowActivity {

    private static final Logger log = LoggerFactory.getLogger(ShipOrderActivity.class);

    private final ShippingServiceGrpc.ShippingServiceBlockingStub shippingStub;

    public ShipOrderActivity(ShippingServiceGrpc.ShippingServiceBlockingStub shippingStub) {
        this.shippingStub = shippingStub;
    }

    @Override
    public Object run(WorkflowActivityContext ctx) {
        Order order = ctx.getInput(Order.class);
        log.info("Requesting shipment for order: {}", order.orderId());

        ShipOrderRequest request = ShipOrderRequest.newBuilder()
                .setOrderId(order.orderId())
                .build();

        ShipOrderResponse response = shippingStub.shipOrder(request);

        log.info("Shipment requested for order: {}, trackingId: {}", order.orderId(), response.getShipmentId());
        return response.getShipmentId();
    }
}
