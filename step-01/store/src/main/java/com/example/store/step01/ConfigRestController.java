package com.example.store.step01;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfigRestController {

    @Value("${store.test-mode:false}")
    private boolean testMode;

    @GetMapping("/api/config")
    public ConfigResponse config() {
        return new ConfigResponse(testMode);
    }

    public record ConfigResponse(boolean testMode) {}
}
