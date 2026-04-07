package com.example.warehouse.mcp.step02.client;

import java.util.List;

public record InventoryResponse(List<ProductResponse> products) {
}