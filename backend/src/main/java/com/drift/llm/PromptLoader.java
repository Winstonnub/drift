package com.drift.llm;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class PromptLoader {

    public String load(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path); // looks for a file inside /resources folder

            return new String(
                resource.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            throw new LlmException("Could not load prompt: " + path, exception);
        }
    }

}