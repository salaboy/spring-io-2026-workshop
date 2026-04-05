package com.example.store.step01.model;

import java.util.List;

public record Order(String orderId, List<MerchItem> items, double totalPrice) {
    public String displayTotalPrice() {
        return String.format("$%.2f", totalPrice);
    }
}
