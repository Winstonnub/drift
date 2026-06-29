package com.drift.llm;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnthropicClient {

    private final RestClient anthropicRestClient; // HTTP client from LlmConfig

    private final AnthropicProperties properties;

    public String createMessage(String systemPrompt, String userPrompt) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) { // missing api key
            throw new LlmException("ANTHROPIC_API_KEY is not configured");
        }

        AnthropicMessagesRequest request = new AnthropicMessagesRequest(
            properties.model(),
            properties.maxTokens(),
            systemPrompt,
            List.of(new AnthropicMessage("user", userPrompt)) // send one user message containing syllabus prompt
        );

        try {
            AnthropicMessageResponse response = anthropicRestClient
                .post()
                .uri("/v1/messages") // calls POST /v1/messages endpoint of Anthropic API
                .body(request)
                .retrieve() 
                .body(AnthropicMessageResponse.class); // sends request and convert JSON into response record

            if (response == null || response.content() == null || response.content().isEmpty()) {
                throw new LlmException("Anthropic returned an empty response");
            }

            String text = response.content().stream()
                .filter(block -> "text".equals(block.type()))
                .map(AnthropicContentBlock::text)
                .filter(blockText -> blockText != null && !blockText.isBlank())
                .collect(Collectors.joining("\n")) // combine multiple text blocks
                .trim();

            if (text.isBlank()) {
                throw new LlmException("Anthropic response did not contain text content");
            }

            return text;
        } catch (RestClientResponseException exception) {
            throw new LlmException(
                "Anthropic API call failed with status " + exception.getStatusCode().value(),
                exception
            );
        } catch (RestClientException exception) {
            throw new LlmException("Anthropic API call failed", exception);
        }
    }

}