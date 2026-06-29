package com.drift.tracks;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest // starts a small spring test focused on MongoDB
@Testcontainers // does not start whole Drift App. Tells JUnit to start the docker containers before and stop after test
class TrackRepositoryTest {

    @Container // marks MongoDB container belogns to this test class
    static MongoDBContainer mongoDBContainer =
        new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    @DynamicPropertySource // Injects container's MongoDB URI into spring // this method answers with the temporary container address
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private TrackRepository trackRepository;

    @AfterEach
    void cleanUp() {
        trackRepository.deleteAll();
    }

    @Test
    void findsActiveReadyTracksDueForDelivery() {
        Instant now = Instant.parse("2026-06-28T00:00:00Z");

        Track dueTrack = track(
            "Due track",
            TrackStatus.ACTIVE,
            SyllabusGenerationStatus.READY,
            now.minusSeconds(60)
        );

        Track futureTrack = track(
            "Future track",
            TrackStatus.ACTIVE,
            SyllabusGenerationStatus.READY,
            now.plusSeconds(60)
        );

        Track pausedTrack = track(
            "Paused track",
            TrackStatus.PAUSED,
            SyllabusGenerationStatus.READY,
            now.minusSeconds(60)
        );

        Track stillGeneratingTrack = track(
            "Still generating track",
            TrackStatus.ACTIVE,
            SyllabusGenerationStatus.GENERATING,
            now.minusSeconds(60)
        );

        trackRepository.saveAll(List.of(
            dueTrack,
            futureTrack,
            pausedTrack,
            stillGeneratingTrack
        ));

        List<Track> result =
            trackRepository.findByStatusAndSyllabusGenerationStatusAndNextDeliveryAtLessThanEqual(
                TrackStatus.ACTIVE,
                SyllabusGenerationStatus.READY,
                now
            );

        assertThat(result)
            .extracting(Track::getTopic)
            .containsExactly("Due track");
    }

    private Track track(
        String topic,
        TrackStatus status,
        SyllabusGenerationStatus syllabusGenerationStatus,
        Instant nextDeliveryAt
    ) {
        return Track.builder()
            .userId("user-123")
            .topic(topic)
            .targetMinutes(5)
            .status(status)
            .deliveryTime("08:00")
            .timezone("Asia/Singapore")
            .channels(List.of("web"))
            .nextDeliveryAt(nextDeliveryAt)
            .syllabusPointer(0)
            .syllabusGenerationStatus(syllabusGenerationStatus)
            .createdAt(Instant.parse("2026-06-27T00:00:00Z"))
            .build();
    }
}