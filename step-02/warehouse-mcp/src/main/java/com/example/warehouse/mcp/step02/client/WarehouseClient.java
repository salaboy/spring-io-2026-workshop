package com.example.warehouse.mcp.step02.client;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestClient;

@Component
public class WarehouseClient {
    @Autowired
    @Qualifier("warehouseRestClient")
    RestClient restClient;

    public InventoryResponse getInventory() {
        return restClient.get()
                .uri("/inventory")
                .retrieve()
                .body(InventoryResponse.class);
    }

    public ProductResponse getProduct(String productId) {
        return restClient.get()
                .uri("/inventory/{productId}", productId)
                .retrieve()
                .body(ProductResponse.class);
    }

    public boolean acquireProduct(String productId, int quantity) {
        AcquireRequest request = new AcquireRequest(quantity);
        restClient.post()
            .uri("/inventory/{productId}/acquire", productId)
            .body(request)
            .retrieve()
            .toBodilessEntity();
        return true;
    }

    public record AcquireRequest(int quantity) {
    }
}