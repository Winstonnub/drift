package com.drift.kafka;

import com.drift.feedback.FeedbackSignal;
import com.drift.kafka.events.FeedbackSubmittedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class FeedbackProducer {

    private static final Logger log = LoggerFactory.getLogger(FeedbackProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishFeedbackSubmitted(
        String userId,
        String trackId,
        String lessonId,
        FeedbackSignal signal,
        String freeText,
        Instant submittedAt
    ) {
        FeedbackSubmittedEvent event = new FeedbackSubmittedEvent(
            userId,
            trackId,
            lessonId,
            signal,
            freeText,
            submittedAt
        );

        kafkaTemplate.send(KafkaTopics.FEEDBACK_SUBMITTED, lessonId, event)
            .whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("Failed to publish feedback event for lesson {}", lessonId, exception);
                    return;
                }

                log.info("Published feedback event for lesson {}", lessonId);
            });
    }

}