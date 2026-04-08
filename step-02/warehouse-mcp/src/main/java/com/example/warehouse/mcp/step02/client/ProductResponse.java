package com.example.warehouse.mcp.step02.client;

public record ProductResponse(String projectName, String productType, int quantity, double price, String logoUrl) {
}