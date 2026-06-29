package com.drift.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.resend") // maps the YAML config
public record ResendProperties(

    String baseUrl,

    String apiKey,

    String from,

    String appUrl // also SpringBoot maps kebab-case yaml to camelCase

) {
}