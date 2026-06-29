package com.drift.kafka;

import com.drift.kafka.events.SyllabusGenerateEvent;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class SyllabusGenerationProducerTest {

    @Container
    static KafkaContainer kafkaContainer = // starts a real kafka broker in docker
        new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Test
    void publishesSyllabusGenerationEventToKafka() {
        Map<String, Object> producerProperties =
            KafkaTestUtils.producerProps(kafkaContainer.getBootstrapServers());

        producerProperties.put(
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            StringSerializer.class
        );
        producerProperties.put(
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            JsonSerializer.class
        );

        KafkaTemplate<String, Object> kafkaTemplate = new KafkaTemplate<>( // the same Spring Helper the real app uses to publish kafka msgs
            new DefaultKafkaProducerFactory<>(producerProperties)
        );

        SyllabusGenerationProducer producer =
            new SyllabusGenerationProducer(kafkaTemplate);

        Map<String, Object> consumerProperties =
            KafkaTestUtils.consumerProps(
                kafkaContainer.getBootstrapServers(),
                "syllabus-producer-test",
                "true"
            );

        consumerProperties.put(
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
            "earliest"
        );
        consumerProperties.put(
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            StringDeserializer.class
        );
        consumerProperties.put(
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            JsonDeserializer.class
        );
        consumerProperties.put(
            JsonDeserializer.TRUSTED_PACKAGES,
            "com.drift.kafka.events"
        );
        consumerProperties.put(
            JsonDeserializer.VALUE_DEFAULT_TYPE,
            SyllabusGenerateEvent.class.getName()
        );

        Consumer<String, SyllabusGenerateEvent> consumer =
            new DefaultKafkaConsumerFactory<String, SyllabusGenerateEvent>(consumerProperties)
                .createConsumer();

        try {
            consumer.subscribe(List.of(KafkaTopics.SYLLABUS_GENERATE)); // wait for one msg to appear

            producer.requestSyllabusGeneration("track-123");
            kafkaTemplate.flush();

            ConsumerRecord<String, SyllabusGenerateEvent> record =
                KafkaTestUtils.getSingleRecord( // waits for one msg to appear
                    consumer,
                    KafkaTopics.SYLLABUS_GENERATE,
                    Duration.ofSeconds(10)
                );

            assertThat(record.key()).isEqualTo("track-123");
            assertThat(record.value().trackId()).isEqualTo("track-123");
        } finally {
            consumer.close();
            kafkaTemplate.destroy();
        }
    }
}