package com.example.store.step02;

import com.example.store.step02.model.MerchItem;
import com.example.store.step02.model.Order;
import com.example.store.step02.model.OrderLine;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ChatController {

    @Tool(description = "Display visual product cards for Spring merch items with their project logos in the UI. "
            + "Pass a project name (e.g. 'Spring Boot'), a type (e.g. 'T-Shirt', 'Socks', 'Sticker'), or 'all' to show everything.")
    public String displayMerchImages(String query, List<MerchItem> items) {
        String q = query.toLowerCase();
        items = items.stream()
                    .filter(item -> item.displayName().toLowerCase().contains(q)
                            || item.projectName().toLowerCase().contains(q)
                            || item.type().toLowerCase().contains(q))
                    .toList();
        String json = items.stream()
                .map(item -> String.format(
                        "{\"projectName\":\"%s\",\"type\":\"%s\",\"price\":%.2f,\"stock\":%d,\"logoUrl\":\"%s\"}",
                        item.projectName(), item.type(), item.price(), item.quantity(), item.logoUrl()))
                .collect(Collectors.joining(",", "[", "]"));
        return "<merch-items>" + json + "</merch-items>";
    }

    @Tool(description = "Place a confirmed order for one or more Spring merch items. "
            + "Call this only after the user has explicitly confirmed they want to place the order. "
            + "Each line must include the project name, type (T-Shirt, Socks, or Sticker), and quantity.")

    public String placeOrder(List<OrderLine> items, List<MerchItem> inventory) {
        List<MerchItem> orderedItems = new ArrayList<>();
        double total = 0.0;
        List<String> notFound = new ArrayList<>();

        for (OrderLine line : items) {
            MerchItem match = inventory.stream()
                    .filter(inv -> inv.projectName().equalsIgnoreCase(line.projectName())
                            && inv.type().equalsIgnoreCase(line.type()))
                    .findFirst()
                    .orElse(null);

            if (match == null) {
                notFound.add(line.projectName() + " " + line.type());
                continue;
            }
            for (int i = 0; i < line.quantity(); i++) {
                orderedItems.add(match);
            }
            total += match.price() * line.quantity();
        }

        if (!notFound.isEmpty()) {
            return "Could not place order — the following items were not found in the catalog: "
                    + String.join(", ", notFound);
        }

        String orderId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Order order = new Order(orderId, orderedItems, total);
        System.out.println("Print Order: " + order);
        return String.format(
                "Your order #%s has been placed successfully! 🎉%n" +
                        "Items: %s%n" +
                        "Total: %s%n" +
                        "It will be shipped to you as soon as possible. Thank you for shopping at the Spring Merch Store!",
                order.orderId(),
                orderedItems.stream()
                        .collect(Collectors.groupingBy(MerchItem::displayName, Collectors.counting()))
                        .entrySet().stream()
                        .map(e -> e.getValue() + "x " + e.getKey())
                        .collect(Collectors.joining(", ")),
                order.displayTotalPrice()
        );
    }

    @Tool(description = "List all available Spring project merch items (T-Shirts, Socks, Stickers) with their quantities and prices")
    public String listAllItems(List<MerchItem> inventory) {
        StringBuilder sb = new StringBuilder("Current Spring merch inventory:\n");
        String currentProject = "";
        for (MerchItem item : inventory) {
            if (!item.projectName().equals(currentProject)) {
                currentProject = item.projectName();
                sb.append("\n").append(currentProject).append(" (").append(item.logoUrl()).append(")\n");
            }
            sb.append(String.format("  - %s: %d units at $%.2f%n", item.type(), item.quantity(), item.price()));
        }
        return sb.toString();
    }
}
