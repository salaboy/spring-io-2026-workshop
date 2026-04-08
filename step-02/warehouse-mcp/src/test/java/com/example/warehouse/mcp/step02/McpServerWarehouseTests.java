package com.example.warehouse.mcp.step02;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {TestMcpServerWarehouseApplication.class, ContainersConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class McpServerWarehouseTests {


    @Test
    void testMcpServerWithMockWarehouse()  {
        //@TODO: add tests to validate the mcp server with mock warehouse
        // working with canned responses

    }
}
