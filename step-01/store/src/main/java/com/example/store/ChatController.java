package com.example.store;

import com.example.store.model.MerchItem;
import com.example.store.model.Order;
import com.example.store.model.OrderLine;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ChatController {

    static final List<MerchItem> INVENTORY = List.of(
            new MerchItem("Spring Boot",       "T-Shirt", 50,  29.99, "https://spring.io/img/projects/spring-boot.svg"),
            new MerchItem("Spring Boot",       "Socks",  100,  12.99, "https://spring.io/img/projects/spring-boot.svg"),
            new MerchItem("Spring Boot",       "Sticker", 200,  4.99, "https://spring.io/img/projects/spring-boot.svg"),
            new MerchItem("Spring AI",         "T-Shirt",  30, 29.99, "https://spring.io/img/projects/spring-ai.svg"),
            new MerchItem("Spring AI",         "Socks",    75, 12.99, "https://spring.io/img/projects/spring-ai.svg"),
            new MerchItem("Spring AI",         "Sticker", 150,  4.99, "https://spring.io/img/projects/spring-ai.svg"),
            new MerchItem("Spring Security",   "T-Shirt",  40, 29.99, "https://spring.io/img/projects/spring-security.svg"),
            new MerchItem("Spring Security",   "Socks",    80, 12.99, "https://spring.io/img/projects/spring-security.svg"),
            new MerchItem("Spring Security",   "Sticker", 175,  4.99, "https://spring.io/img/projects/spring-security.svg"),
            new MerchItem("Spring Cloud",      "T-Shirt",  35, 29.99, "https://spring.io/img/projects/spring-cloud.svg"),
            new MerchItem("Spring Cloud",      "Socks",    90, 12.99, "https://spring.io/img/projects/spring-cloud.svg"),
            new MerchItem("Spring Cloud",      "Sticker", 160,  4.99, "https://spring.io/img/projects/spring-cloud.svg"),
            new MerchItem("Spring Data",       "T-Shirt",  25, 29.99, "https://spring.io/img/projects/spring-data.svg"),
            new MerchItem("Spring Data",       "Socks",    60, 12.99, "https://spring.io/img/projects/spring-data.svg"),
            new MerchItem("Spring Data",       "Sticker", 140,  4.99, "https://spring.io/img/projects/spring-data.svg"),
            new MerchItem("Spring Batch",      "T-Shirt",  20, 29.99, "https://spring.io/img/projects/spring-batch.svg"),
            new MerchItem("Spring Batch",      "Socks",    55, 12.99, "https://spring.io/img/projects/spring-batch.svg"),
            new MerchItem("Spring Batch",      "Sticker", 120,  4.99, "https://spring.io/img/projects/spring-batch.svg"),
            new MerchItem("Spring for GraphQL","T-Shirt",  15, 29.99, "https://spring.io/img/projects/spring-graphql.svg"),
            new MerchItem("Spring for GraphQL","Socks",    40, 12.99, "https://spring.io/img/projects/spring-graphql.svg"),
            new MerchItem("Spring for GraphQL","Sticker", 100,  4.99, "https://spring.io/img/projects/spring-graphql.svg"),
            new MerchItem("Spring Modulith",   "T-Shirt",  10, 29.99, "https://spring.io/img/projects/spring-modulith.svg"),
            new MerchItem("Spring Modulith",   "Socks",    30, 12.99, "https://spring.io/img/projects/spring-modulith.svg"),
            new MerchItem("Spring Modulith",   "Sticker",  80,  4.99, "https://spring.io/img/projects/spring-modulith.svg"),
            new MerchItem("Spring Authorization Server", "T-Shirt",  18, 29.99, "https://spring.io/img/projects/spring-authorization-server.svg"),
            new MerchItem("Spring Authorization Server", "Sticker",  90,  4.99, "https://spring.io/img/projects/spring-authorization-server.svg"),
            new MerchItem("Spring Session",    "T-Shirt",  12, 29.99, "https://spring.io/img/projects/logo-session.png"),
            new MerchItem("Spring Session",    "Sticker",  70,  4.99, "https://spring.io/img/projects/logo-session.png"),
            new MerchItem("Spring Statemachine","T-Shirt",  8, 29.99, "https://spring.io/img/projects/spring-statemachine.svg"),
            new MerchItem("Spring Statemachine","Sticker",  60,  4.99, "https://spring.io/img/projects/spring-statemachine.svg")
    );

    @Tool(description = "Get the stock quantity and price of a Spring merch item by project name and/or type (T-Shirt, Socks, Sticker)")
    public String getItemStock(String itemName) {
        String query = itemName.toLowerCase();
        List<MerchItem> matches = INVENTORY.stream()
                .filter(item -> item.displayName().toLowerCase().contains(query)
                        || item.projectName().toLowerCase().contains(query)
                        || item.type().toLowerCase().contains(query))
                .toList();

        if (matches.isEmpty()) {
            return "No merch found matching '" + itemName + "'";
        }

        return matches.stream()
                .map(item -> String.format("- %s: %d units in stock at $%.2f (logo: %s)",
                        item.displayName(), item.quantity(), item.price(), item.logoUrl()))
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = "Display visual product cards for Spring merch items with their project logos in the UI. "
            + "Pass a project name (e.g. 'Spring Boot'), a type (e.g. 'T-Shirt', 'Socks', 'Sticker'), or 'all' to show everything.")
    public String displayMerchImages(String query) {
        List<MerchItem> items;
        if ("all".equalsIgnoreCase(query.trim())) {
            items = INVENTORY;
        } else {
            String q = query.toLowerCase();
            items = INVENTORY.stream()
                    .filter(item -> item.displayName().toLowerCase().contains(q)
                            || item.projectName().toLowerCase().contains(q)
                            || item.type().toLowerCase().contains(q))
                    .toList();
        }
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

    public String placeOrder(List<OrderLine> items) {
        List<MerchItem> orderedItems = new ArrayList<>();
        double total = 0.0;
        List<String> notFound = new ArrayList<>();

        for (OrderLine line : items) {
            MerchItem match = INVENTORY.stream()
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
    public String listAllItems() {
        StringBuilder sb = new StringBuilder("Current Spring merch inventory:\n");
        String currentProject = "";
        for (MerchItem item : INVENTORY) {
            if (!item.projectName().equals(currentProject)) {
                currentProject = item.projectName();
                sb.append("\n").append(currentProject).append(" (").append(item.logoUrl()).append(")\n");
            }
            sb.append(String.format("  - %s: %d units at $%.2f%n", item.type(), item.quantity(), item.price()));
        }
        return sb.toString();
    }
}
