package com.example.warehouse.step02;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final Map<String, Product> inventory = new ConcurrentHashMap<>();

    public InventoryController() {
        inventory.put("Spring Boot T-Shirt", new Product("Spring Boot", "T-Shirt", 50, 29.99, "https://spring.io/img/projects/spring-boot.svg"));
        inventory.put("Spring Boot Socks", new Product("Spring Boot", "Socks", 100, 12.99, "https://spring.io/img/projects/spring-boot.svg"));
        inventory.put("Spring Boot Sticker", new Product("Spring Boot", "Sticker", 200, 4.99, "https://spring.io/img/projects/spring-boot.svg"));
        inventory.put("Spring AI T-Shirt", new Product("Spring AI", "T-Shirt", 30, 29.99, "https://spring.io/img/projects/spring-ai.svg"));
        inventory.put("Spring AI Socks", new Product("Spring AI", "Socks", 75, 12.99, "https://spring.io/img/projects/spring-ai.svg"));
        inventory.put("Spring AI Sticker", new Product("Spring AI", "Sticker", 150, 4.99, "https://spring.io/img/projects/spring-ai.svg"));
        inventory.put("Spring Security T-Shirt", new Product("Spring Security", "T-Shirt", 40, 29.99, "https://spring.io/img/projects/spring-security.svg"));
        inventory.put("Spring Security Socks", new Product("Spring Security", "Socks", 80, 12.99, "https://spring.io/img/projects/spring-security.svg"));
        inventory.put("Spring Security Sticker", new Product("Spring Security", "Sticker", 175, 4.99, "https://spring.io/img/projects/spring-security.svg"));
        inventory.put("Spring Cloud T-Shirt", new Product("Spring Cloud", "T-Shirt", 35, 29.99, "https://spring.io/img/projects/spring-cloud.svg"));
        inventory.put("Spring Cloud Socks", new Product("Spring Cloud", "Socks", 90, 12.99, "https://spring.io/img/projects/spring-cloud.svg"));
        inventory.put("Spring Cloud Sticker", new Product("Spring Cloud", "Sticker", 160, 4.99, "https://spring.io/img/projects/spring-cloud.svg"));
        inventory.put("Spring Data T-Shirt", new Product("Spring Data", "T-Shirt", 25, 29.99, "https://spring.io/img/projects/spring-data.svg"));
        inventory.put("Spring Data Socks", new Product("Spring Data", "Socks", 60, 12.99, "https:// spring.io/img/projects/spring-data.svg"));
        inventory.put("Spring Data Sticker", new Product("Spring Data", "Sticker", 140, 4.99, "https://spring.io/img/projects/spring-data.svg"));
        inventory.put("Spring Batch T-Shirt", new Product("Spring Batch", "T-Shirt", 20, 29.99, "https://spring.io/img/projects/spring-batch.svg"));
        inventory.put("Spring Batch Socks", new Product("Spring Batch", "Socks", 55, 12.99, "https://spring.io/img/projects/spring-batch.svg"));
        inventory.put("Spring Batch Sticker", new Product("Spring Batch", "Sticker", 120, 4.99, "https://spring.io/img/projects/spring-batch.svg"));
        inventory.put("Spring for GraphQL T-Shirt", new Product("Spring for GraphQL", "T-Shirt", 15, 29.99, "https://spring.io/img/projects/spring-graphql.svg"));
        inventory.put("Spring for GraphQL Socks", new Product("Spring for GraphQL", "Socks", 40, 12.99, "https://spring.io/img/projects/spring-graphql.svg"));
        inventory.put("Spring for GraphQL Sticker", new Product("Spring for GraphQL", "Sticker", 100, 4.99, "https://spring.io/img/projects/spring-graphql.svg"));
        inventory.put("Spring Modulith T-Shirt", new Product("Spring Modulith", "T-Shirt", 10, 29.99, "https://spring.io/img/projects/spring-modulith.svg"));
        inventory.put("Spring Modulith Socks", new Product("Spring Modulith", "Socks", 30, 12.99, "https://spring.io/img/projects/spring-modulith.svg"));
        inventory.put("Spring Modulith Sticker", new Product("Spring Modulith", "Sticker", 80, 4.99, "https://spring.io/img/projects/spring-modulith.svg"));
        inventory.put("Spring Authorization Server T-Shirt", new Product("Spring Authorization Server", "T-Shirt", 18, 29.99, "https://spring.io/img/projects/spring-authorization-server.svg"));
        inventory.put("Spring Authorization Server Sticker", new Product("Spring Authorization Server", "Sticker", 90, 4.99, "https://spring.io/img/projects/spring-authorization-server.svg"));
        inventory.put("Spring Session T-Shirt", new Product("Spring Session", "T-Shirt", 12, 29.99, "https://spring.io/img/projects/logo-session.png"));
        inventory.put("Spring Session Sticker", new Product("Spring Session", "Sticker", 70, 4.99, "https://spring.io/img/projects/logo-session.png"));
        inventory.put("Spring Statemachine T-Shirt", new Product("Spring Statemachine", "T-Shirt", 8, 29.99, "https://spring.io/img/projects/spring-statemachine.svg"));
        inventory.put("Spring Statemachine Sticker", new Product("Spring Statemachine", "Sticker", 60, 4.99, "https://spring.io/img/projects/spring-statemachine.svg"));
    }

    @GetMapping
    public InventoryResponse getInventory() {
        List<ProductResponse> products = inventory.values().stream()
                .map(p -> new ProductResponse(p.projectName(), p.productType(), p.quantity(), p.price(), p.logoUrl()))
                .toList();
        return new InventoryResponse(products);
    }

    @GetMapping("/{projectName}/{productType}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable String projectName, @PathVariable String productType) {
        String productId = projectName + " " + productType;
        Product product = inventory.get(productId);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new ProductResponse(product.projectName(), product.productType(), product.quantity(), product.price(), product.logoUrl()));
    }

    @PostMapping("/{projectName}/{productType}/acquire")
    public ResponseEntity<Void> acquireProduct(@PathVariable String projectName, @PathVariable String productType, @RequestBody AcquireRequest request) {
        String productId = projectName + " " + productType;
        Product product = inventory.get(productId);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        int newQuantity = product.quantity() - request.quantity();
        if (newQuantity < 0) {
            return ResponseEntity.badRequest().build();
        }
        inventory.put(productId, new Product(product.projectName(), product.productType(), newQuantity, product.price(), product.logoUrl()));
        return ResponseEntity.ok().build();
    }

    private record Product(String projectName, String productType, int quantity, double price, String logoUrl) {
    }

    public record AcquireRequest(int quantity) {
}

    public record InventoryResponse(List<ProductResponse> products) {
    }

    public record ProductResponse(String projectName, String productType, int quantity, double price, String logoUrl) {
    }
}