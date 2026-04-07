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

    public ProductResponse getProduct(String projectName, String productType) {
        return restClient.get()
                .uri("/inventory/{projectName}/{productType}", projectName, productType)
                .retrieve()
                .body(ProductResponse.class);
    }

    public boolean acquireProduct(String projectName, String productType, int quantity) {
        AcquireRequest request = new AcquireRequest(quantity);
        restClient.post()
            .uri("/inventory/{projectName}/{productType}/acquire", projectName, productType)
            .body(request)
            .retrieve()
            .toBodilessEntity();
        return true;
    }

    public record AcquireRequest(int quantity) {
    }
}