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
    //    static final List<MerchItem> INVENTORY = List.of(
//            new MerchItem("Spring Boot",       "T-Shirt", 50,  29.99, "https://spring.io/img/projects/spring-boot.svg"),
//            new MerchItem("Spring Boot",       "Socks",  100,  12.99, "https://spring.io/img/projects/spring-boot.svg"),
//            new MerchItem("Spring Boot",       "Sticker", 200,  4.99, "https://spring.io/img/projects/spring-boot.svg"),
//            new MerchItem("Spring AI",         "T-Shirt",  30, 29.99, "https://spring.io/img/projects/spring-ai.svg"),
//            new MerchItem("Spring AI",         "Socks",    75, 12.99, "https://spring.io/img/projects/spring-ai.svg"),
//            new MerchItem("Spring AI",         "Sticker", 150,  4.99, "https://spring.io/img/projects/spring-ai.svg"),
//            new MerchItem("Spring Security",   "T-Shirt",  40, 29.99, "https://spring.io/img/projects/spring-security.svg"),
//            new MerchItem("Spring Security",   "Socks",    80, 12.99, "https://spring.io/img/projects/spring-security.svg"),
//            new MerchItem("Spring Security",   "Sticker", 175,  4.99, "https://spring.io/img/projects/spring-security.svg"),
//            new MerchItem("Spring Cloud",      "T-Shirt",  35, 29.99, "https://spring.io/img/projects/spring-cloud.svg"),
//            new MerchItem("Spring Cloud",      "Socks",    90, 12.99, "https://spring.io/img/projects/spring-cloud.svg"),
//            new MerchItem("Spring Cloud",      "Sticker", 160,  4.99, "https://spring.io/img/projects/spring-cloud.svg"),
//            new MerchItem("Spring Data",       "T-Shirt",  25, 29.99, "https://spring.io/img/projects/spring-data.svg"),
//            new MerchItem("Spring Data",       "Socks",    60, 12.99, "https://spring.io/img/projects/spring-data.svg"),
//            new MerchItem("Spring Data",       "Sticker", 140,  4.99, "https://spring.io/img/projects/spring-data.svg"),
//            new MerchItem("Spring Batch",      "T-Shirt",  20, 29.99, "https://spring.io/img/projects/spring-batch.svg"),
//            new MerchItem("Spring Batch",      "Socks",    55, 12.99, "https://spring.io/img/projects/spring-batch.svg"),
//            new MerchItem("Spring Batch",      "Sticker", 120,  4.99, "https://spring.io/img/projects/spring-batch.svg"),
//            new MerchItem("Spring for GraphQL","T-Shirt",  15, 29.99, "https://spring.io/img/projects/spring-graphql.svg"),
//            new MerchItem("Spring for GraphQL","Socks",    40, 12.99, "https://spring.io/img/projects/spring-graphql.svg"),
//            new MerchItem("Spring for GraphQL","Sticker", 100,  4.99, "https://spring.io/img/projects/spring-graphql.svg"),
//            new MerchItem("Spring Modulith",   "T-Shirt",  10, 29.99, "https://spring.io/img/projects/spring-modulith.svg"),
//            new MerchItem("Spring Modulith",   "Socks",    30, 12.99, "https://spring.io/img/projects/spring-modulith.svg"),
//            new MerchItem("Spring Modulith",   "Sticker",  80,  4.99, "https://spring.io/img/projects/spring-modulith.svg"),
//            new MerchItem("Spring Authorization Server", "T-Shirt",  18, 29.99, "https://spring.io/img/projects/spring-authorization-server.svg"),
//            new MerchItem("Spring Authorization Server", "Sticker",  90,  4.99, "https://spring.io/img/projects/spring-authorization-server.svg"),
//            new MerchItem("Spring Session",    "T-Shirt",  12, 29.99, "https://spring.io/img/projects/logo-session.png"),
//            new MerchItem("Spring Session",    "Sticker",  70,  4.99, "https://spring.io/img/projects/logo-session.png"),
//            new MerchItem("Spring Statemachine","T-Shirt",  8, 29.99, "https://spring.io/img/projects/spring-statemachine.svg"),
//            new MerchItem("Spring Statemachine","Sticker",  60,  4.99, "https://spring.io/img/projects/spring-statemachine.svg")
//    );
    public InventoryController() {
        inventory.put("Spring Boot T-Shirt", new Product("Spring Boot", "T-Shirt", "Nice t-shirt featuring Spring Boot", 50));
        inventory.put("Spring Boot Socks", new Product("Spring Boot", "Socks", "Comfortable socks with Spring Boot logo", 100));
        inventory.put("Spring Boot Sticker", new Product("Spring Boot", "Sticker", "Stylish sticker with Spring Boot branding", 200));
    }

    @GetMapping
    public InventoryResponse getInventory() {
        List<ProductResponse> products = inventory.values().stream()
                .map(p -> new ProductResponse(p.projectName(), p.productType(), p.description(), p.quantity()))
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
        return ResponseEntity.ok(new ProductResponse(product.projectName(), product.productType(), product.description(), product.quantity()));
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
        inventory.put(productId, new Product(product.projectName(), product.productType(), product.description(), newQuantity));
        return ResponseEntity.ok().build();
    }

    private record Product(String projectName, String productType, String description, int quantity) {
    }

    public record AcquireRequest(int quantity) {
}

    public record InventoryResponse(List<ProductResponse> products) {
    }

    public record ProductResponse(String projectName, String productType, String description, int quantity) {
    }
}