package com.example.warehouse.mcp.step02;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp.OtlpMetricsExportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@SpringBootApplication(exclude = OtlpMetricsExportAutoConfiguration.class)
@ConfigurationPropertiesScan
public class McpServerWarehouseApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerWarehouseApplication.class, args);
    }

    @Bean("warehouseRestClient")
    RestClient warehouseRestClient(ApplicationProperties properties) {
		return RestClient.create(properties.warehouseBaseUrl());
	}
}