package com.example.warehouse.step02;

import io.github.microcks.testcontainers.MicrocksContainer;
import io.github.microcks.testcontainers.model.RequestResponsePair;
import io.github.microcks.testcontainers.model.TestRequest;
import io.github.microcks.testcontainers.model.TestResult;
import io.github.microcks.testcontainers.model.TestRunnerType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.testcontainers.Testcontainers;

import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"microcks.enabled=true"},
        classes = TestWarehouseApplication.class)
@Import(ContainersConfig.class)
class WarehouseAPIContractTests {
    
    @Autowired
    protected MicrocksContainer microcks;

    @LocalServerPort
    protected Integer port;

    @BeforeEach
    void setupPort() {
       // Host port exposition should be done here.
       Testcontainers.exposeHostPorts(port);
    }

    @Test
    void shouldExposeConformantWarehouseAPIEndpoint() throws Exception {
        // Ask for an Open API conformance to be launched.
        TestRequest testRequest = new TestRequest.Builder()
            .serviceId("Warehouse API:1.0.0")
            .runnerType(TestRunnerType.OPEN_API_SCHEMA.name())
            .testEndpoint("http://host.testcontainers.internal:" + port)
            .build();

        TestResult testResult = microcks.testEndpoint(testRequest);

        // You may inspect complete response object with following:
        ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(testResult));

        assertTrue(testResult.isSuccess());
        assertEquals(3, testResult.getTestCaseResults().size());
        
        // Collect the total number of test steps executed across all test cases.
        int totalSteps = (int) testResult.getTestCaseResults().stream()
            .flatMap(testCaseResult -> testCaseResult.getTestStepResults().stream())
            .count();
        assertEquals(5, totalSteps);
    }
}