package com.drift.llm;

import com.drift.syllabus.SyllabusItem;
import com.drift.syllabus.SyllabusItemStatus;
import com.drift.tracks.Track;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LlmService {

    private static final int EXPECTED_SYLLABUS_ITEM_COUNT = 30;

    private static final int WORDS_PER_MINUTE = 200;

    private static final String SYLLABUS_SYSTEM_PROMPT_PATH = "prompts/syllabus_system.md";

    private static final String SYLLABUS_USER_PROMPT_PATH = "prompts/syllabus_user.md";

    private static final String LESSON_SYSTEM_PROMPT_PATH = "prompts/lesson_system.md";

    private static final String LESSON_USER_PROMPT_PATH = "prompts/lesson_user.md";

    private final AnthropicClient anthropicClient;

    private final PromptLoader promptLoader;

    private final ObjectMapper objectMapper;

    public List<SyllabusItem> generateSyllabus(String topic) {
        String systemPrompt = promptLoader.load(SYLLABUS_SYSTEM_PROMPT_PATH);

        String userPrompt = promptLoader.load(SYLLABUS_USER_PROMPT_PATH)
            .replace("{{topic}}", topic);

        String responseText = anthropicClient.createMessage(systemPrompt, userPrompt);

        GeneratedSyllabusResponse generatedSyllabus = parseGeneratedSyllabus(responseText);

        validateGeneratedSyllabus(generatedSyllabus);

        return generatedSyllabus.items().stream()
            .sorted(Comparator.comparing(GeneratedSyllabusItem::index))
            .map(this::toSyllabusItem)
            .toList();
    }

    public String generateLesson(Track track, SyllabusItem syllabusItem) {
        int wordBudget = track.getTargetMinutes() * WORDS_PER_MINUTE;

        String systemPrompt = promptLoader.load(LESSON_SYSTEM_PROMPT_PATH);

        String userPrompt = promptLoader.load(LESSON_USER_PROMPT_PATH)
            .replace("{{topic}}", track.getTopic())
            .replace("{{targetMinutes}}", track.getTargetMinutes().toString())
            .replace("{{wordBudget}}", Integer.toString(wordBudget))
            .replace("{{title}}", syllabusItem.getTitle())
            .replace("{{summary}}", syllabusItem.getSummary())
            .replace("{{prerequisites}}", String.join(", ", syllabusItem.getPrerequisites()));

        String lessonMarkdown = anthropicClient.createMessage(systemPrompt, userPrompt).trim();

        if (lessonMarkdown.isBlank()) {
            throw new LlmException("LLM returned an empty lesson");
        }

        return lessonMarkdown;
    }

    private GeneratedSyllabusResponse parseGeneratedSyllabus(String responseText) {
        String json = extractJsonObject(responseText);

        try {
            return objectMapper.readValue(json, GeneratedSyllabusResponse.class);
        } catch (JsonProcessingException exception) {
            throw new LlmException("LLM returned invalid syllabus JSON", exception);
        }
    }

    private String extractJsonObject(String responseText) {
        int startIndex = responseText.indexOf('{');
        int endIndex = responseText.lastIndexOf('}');

        if (startIndex == -1 || endIndex == -1 || endIndex < startIndex) {
            throw new LlmException("LLM response did not contain a JSON object");
        }

        return responseText.substring(startIndex, endIndex + 1);
    }

    private void validateGeneratedSyllabus(GeneratedSyllabusResponse generatedSyllabus) {
        if (generatedSyllabus.items() == null) {
            throw new LlmException("Generated syllabus is missing items");
        }

        if (generatedSyllabus.items().size() != EXPECTED_SYLLABUS_ITEM_COUNT) {
            throw new LlmException("Generated syllabus must contain exactly 30 items");
        }

        Set<Integer> seenIndexes = new HashSet<>();

        for (GeneratedSyllabusItem item : generatedSyllabus.items()) {
            validateGeneratedSyllabusItem(item, seenIndexes);
        }

        for (int index = 0; index < EXPECTED_SYLLABUS_ITEM_COUNT; index++) {
            if (!seenIndexes.contains(index)) {
                throw new LlmException("Generated syllabus is missing index: " + index);
            }
        }
    }

    private void validateGeneratedSyllabusItem(
        GeneratedSyllabusItem item,
        Set<Integer> seenIndexes
    ) {
        if (item.index() == null) {
            throw new LlmException("Generated syllabus item is missing index");
        }

        if (!seenIndexes.add(item.index())) {
            throw new LlmException("Generated syllabus has duplicate index: " + item.index());
        }

        if (item.index() < 0 || item.index() >= EXPECTED_SYLLABUS_ITEM_COUNT) {
            throw new LlmException("Generated syllabus index is out of range: " + item.index());
        }

        if (item.title() == null || item.title().isBlank()) {
            throw new LlmException("Generated syllabus item is missing title");
        }

        if (item.summary() == null || item.summary().isBlank()) {
            throw new LlmException("Generated syllabus item is missing summary");
        }

        if (item.prerequisites() == null) {
            throw new LlmException("Generated syllabus item is missing prerequisites");
        }
    }

    private SyllabusItem toSyllabusItem(GeneratedSyllabusItem generatedItem) {
        return SyllabusItem.builder()
            .index(generatedItem.index())
            .title(generatedItem.title().trim())
            .summary(generatedItem.summary().trim())
            .prerequisites(generatedItem.prerequisites())
            .status(SyllabusItemStatus.PENDING)
            .build();
    }

}