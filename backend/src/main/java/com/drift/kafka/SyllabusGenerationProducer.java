package com.drift.kafka;

import com.drift.kafka.events.SyllabusGenerateEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SyllabusGenerationProducer {

    private static final Logger log = LoggerFactory.getLogger(SyllabusGenerationProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void requestSyllabusGeneration(String trackId) {
        SyllabusGenerateEvent event = new SyllabusGenerateEvent(trackId);

        kafkaTemplate.send(KafkaTopics.SYLLABUS_GENERATE, trackId, event)
            .whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("Failed to publish syllabus generation event for track {}", trackId, exception);
                    return;
                }

                log.info("Published syllabus generation event for track {}", trackId);
            });
    }

}