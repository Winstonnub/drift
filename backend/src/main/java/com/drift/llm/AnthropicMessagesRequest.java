package com.drift.llm;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AnthropicMessagesRequest(

    String model,

    @JsonProperty("max_tokens")
    Integer maxTokens,

    String system,

    List<AnthropicMessage> messages

) {
}