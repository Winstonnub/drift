package com.drift.common.health; // Package must match folder path

import org.springframework.web.bind.annotation.GetMapping; // Import annotation for handling GET
import org.springframework.web.bind.annotation.RestController; // Import annotation to mark the class as REST controller

@RestController // Create instance of this, register as web controller, convert return into HTTP Response bodies
public class HealthController {

    @GetMapping("/health") // When HTTP Get request to /health, call this method and return the result as HTTP response body
    public HealthResponse health() {
        return new HealthResponse("ok");
    }

}