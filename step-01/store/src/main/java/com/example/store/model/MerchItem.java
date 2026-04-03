package com.example.store.model;

public record MerchItem(String projectName, String type, int quantity, double price, String logoUrl) {
    public String displayName() {
        return projectName + " " + type;
    }
}
