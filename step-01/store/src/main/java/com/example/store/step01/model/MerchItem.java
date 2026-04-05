package com.example.store.step01.model;

public record MerchItem(String projectName, String type, int quantity, double price, String logoUrl) {
    public String displayName() {
        return projectName + " " + type;
    }
}
