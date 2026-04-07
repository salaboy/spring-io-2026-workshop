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