package com.example.warehouse.mcp.step02;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TestMcpServerWarehouseApplication {

    public static void main(String[] args) {
        SpringApplication.from(McpServerWarehouseApplication::main)
                .with(ContainersConfig.class)
                .run(args);
    }
}
