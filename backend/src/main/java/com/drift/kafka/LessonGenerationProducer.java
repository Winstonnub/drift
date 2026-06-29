package com.drift.kafka;

import com.drift.kafka.events.LessonGenerateEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LessonGenerationProducer {

    private static final Logger log = LoggerFactory.getLogger(LessonGenerationProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void requestLessonGeneration(String trackId, boolean manual) {
        LessonGenerateEvent event = new LessonGenerateEvent(
            trackId,
            Instant.now(),
            manual
        );

        kafkaTemplate.send(KafkaTopics.LESSON_GENERATE, trackId, event)
            .whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("Failed to publish lesson generation event for track {}", trackId, exception);
                    return;
                }

                log.info("Published lesson generation event for track {}", trackId);
            });
    }

}