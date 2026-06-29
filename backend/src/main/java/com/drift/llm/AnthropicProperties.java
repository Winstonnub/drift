package com.drift.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.anthropic") // maps properties with prefix "app.anthropic" in application.yml to this class
public record AnthropicProperties(

    String baseUrl,

    String apiKey,

    String version,

    String model,

    Integer maxTokens

) {
}