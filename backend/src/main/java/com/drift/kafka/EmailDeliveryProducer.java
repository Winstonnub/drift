package com.drift.kafka;

import com.drift.kafka.events.EmailDeliveryEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EmailDeliveryProducer {

    private static final Logger log = LoggerFactory.getLogger(EmailDeliveryProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void requestEmailDelivery(String lessonId) {
        EmailDeliveryEvent event = new EmailDeliveryEvent(
            lessonId,
            Instant.now()
        );

        kafkaTemplate.send(KafkaTopics.LESSON_DELIVER_EMAIL, lessonId, event)
            .whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("Failed to publish email delivery event for lesson {}", lessonId, exception);
                    return;
                }

                log.info("Published email delivery event for lesson {}", lessonId);
            });
    }

}