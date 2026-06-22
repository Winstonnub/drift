package com.drift;// Test lives in com.drift, let it find main DriftApplication

import org.junit.jupiter.api.Test; // Import JUnit 5 @Test annotation
import org.springframework.beans.factory.annotation.Autowired; // Let Spring inject obj in test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc; // Auto-configure MockMvc instance for testing web layer
import org.springframework.boot.test.context.SpringBootTest; // Start Spring Application context for the test
import org.springframework.test.web.servlet.MockMvc; // test without opening real network port

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTest {

    @Autowired // Spring injects test HTTP client here
    private MockMvc mockMvc; // MockMvc allows us to send HTTP requests in tests without starting a server

    @Test
    void healthEndpointReturnsOk() throws Exception {
        mockMvc.perform(get("/health")) // Send GET request to /health
                .andExpect(status().isOk()) // Expect HTTP 200 OK status
                .andExpect(content().json("{\"status\":\"ok\"}")); // Expect JSON response with status "ok"
    }
}
