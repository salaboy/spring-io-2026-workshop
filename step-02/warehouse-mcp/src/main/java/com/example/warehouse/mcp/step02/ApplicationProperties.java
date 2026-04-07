package com.example.warehouse.mcp.step02;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "application")
@Validated
public record ApplicationProperties(String warehouseBaseUrl) {}