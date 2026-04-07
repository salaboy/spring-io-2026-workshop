package com.example.warehouse.mcp.step02;

import com.example.warehouse.mcp.step02.client.InventoryResponse;
import com.example.warehouse.mcp.step02.client.ProductResponse;
import com.example.warehouse.mcp.step02.client.WarehouseClient;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;

import org.springframework.stereotype.Service;

@Service
public class WarehouseMcpService {

    com.example.warehouse.mcp.step02.client.WarehouseClient warehouseClient;

    public WarehouseMcpService(WarehouseClient warehouseClient) {
        this.warehouseClient = warehouseClient;
    }

    @McpTool(
        name= "list_inventory",
        description = "Get the inventory of products in the warehouse")
    public InventoryResponse getInventory() {
        return warehouseClient.getInventory();
    }

    @McpTool(
        name= "get_product",
        description = "Get details of a specific product in the warehouse")
    public ProductResponse getProduct(@McpToolParam(description = "ID of the product") String productId) {
        return warehouseClient.getProduct(productId);
    }

    @McpTool(
        name= "acquire_product",
        description = "Acquire a specific quantity of a product from the warehouse")
    public boolean acquireProduct(@McpToolParam(description = "ID of the product") String productId,
                                  @McpToolParam(description = "Quantity to acquire") int quantity) {
        return warehouseClient.acquireProduct(productId, quantity);
    }
}