package com.drift.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public KafkaAdmin.NewTopics driftTopics() {
        return new KafkaAdmin.NewTopics(
            topic(KafkaTopics.SYLLABUS_GENERATE),
            topic(KafkaTopics.LESSON_GENERATE),
            topic(KafkaTopics.LESSON_DELIVER_EMAIL),
            topic(KafkaTopics.FEEDBACK_SUBMITTED),
            topic(KafkaTopics.SYLLABUS_GENERATE_DLQ),
            topic(KafkaTopics.LESSON_GENERATE_DLQ),
            topic(KafkaTopics.LESSON_DELIVER_EMAIL_DLQ),
            topic(KafkaTopics.FEEDBACK_SUBMITTED_DLQ)
        );
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (record, exception) -> new TopicPartition(record.topic() + ".dlq", record.partition())
        );

        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name)
            .partitions(3)
            .replicas(1)
            .build();
    }

}