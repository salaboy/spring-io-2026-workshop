package com.example.warehouse.step02;

import io.github.microcks.testcontainers.MicrocksContainer;

import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfig {

    @Bean
    MicrocksContainer microcks() {
        return new MicrocksContainer("quay.io/microcks/microcks-uber:1.13.2-native")
            .withAccessToHost(true)   // We need this to access our webapp while it runs
            .withMainArtifacts("warehouse-openapi-1.0.0.yaml");
    }
}