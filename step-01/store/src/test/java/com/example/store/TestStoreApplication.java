package com.example.store;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TestStoreApplication {

    public static void main(String[] args) {
        SpringApplication.from(StoreApplication::main)
                .with(ContainersConfig.class)
                .run(args);
    }
}
