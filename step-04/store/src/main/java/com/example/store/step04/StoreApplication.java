package com.example.store.step04;

import io.dapr.spring.workflows.config.EnableDaprWorkflows;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp.OtlpMetricsExportAutoConfiguration;

@SpringBootApplication(exclude = OtlpMetricsExportAutoConfiguration.class)
@EnableDaprWorkflows
public class StoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(StoreApplication.class, args);
    }
}
