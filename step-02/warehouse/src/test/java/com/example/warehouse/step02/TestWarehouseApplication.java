package com.example.warehouse.step02;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TestWarehouseApplication {

    public static void main(String[] args) {
        SpringApplication.from(WarehouseApplication::main)
                .with(ContainersConfig.class)
                .run(args);
    }
}
