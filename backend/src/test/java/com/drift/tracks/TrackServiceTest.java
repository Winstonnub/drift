package com.drift.tracks;

import com.drift.kafka.SyllabusGenerationProducer;
import com.drift.syllabus.Syllabus;
import com.drift.syllabus.SyllabusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TrackServiceTest {

    private TrackRepository trackRepository;

    private SyllabusRepository syllabusRepository;

    private SyllabusGenerationProducer syllabusGenerationProducer;

    private TrackService trackService;

    @BeforeEach
    void setUp() {
        trackRepository = mock(TrackRepository.class); // creates a fake Repo, does not talk to MongoDB
        syllabusRepository = mock(SyllabusRepository.class);
        syllabusGenerationProducer = mock(SyllabusGenerationProducer.class);
        trackService = new TrackService(
            trackRepository,
            syllabusRepository,
            syllabusGenerationProducer
        );
    }

    @Test
    void createTrackSavesTrackAndPublishesSyllabusGeneration() {
        // when tells the fake repo what to do when save() is called
        when(syllabusRepository.findByTrackId(anyString())).thenReturn(Optional.empty()); // pretend MongoDB assigned it some id

        when(trackRepository.save(any(Track.class))).thenAnswer(invocation -> {
            Track track = invocation.getArgument(0);
            track.setId("track-123");
            return track;
        });

        CreateTrackRequest request = new CreateTrackRequest(
            "  Kafka in Production  ",
            5,
            "08:00",
            "Asia/Singapore",
            List.of("WEB", "web")
        );

        CreateTrackResponse response = trackService.createTrack("user-123", request);

        ArgumentCaptor<Track> trackCaptor = ArgumentCaptor.forClass(Track.class); // lets the test inspect the exact Track object that TrackService tried to save
        verify(trackRepository).save(trackCaptor.capture());
        // catch the Track obj before it tries to go to the fake Mongodb
        Track savedTrack = trackCaptor.getValue();

        assertThat(savedTrack.getUserId()).isEqualTo("user-123");
        assertThat(savedTrack.getTopic()).isEqualTo("Kafka in Production");
        assertThat(savedTrack.getTargetMinutes()).isEqualTo(5);
        assertThat(savedTrack.getStatus()).isEqualTo(TrackStatus.ACTIVE);
        assertThat(savedTrack.getDeliveryTime()).isEqualTo("08:00");
        assertThat(savedTrack.getTimezone()).isEqualTo("Asia/Singapore");
        assertThat(savedTrack.getChannels()).containsExactly("web");
        assertThat(savedTrack.getSyllabusPointer()).isZero();
        assertThat(savedTrack.getSyllabusGenerationStatus())
            .isEqualTo(SyllabusGenerationStatus.GENERATING);
        assertThat(savedTrack.getCreatedAt()).isNotNull();
        assertThat(savedTrack.getNextDeliveryAt()).isNotNull();

        verify(syllabusGenerationProducer).requestSyllabusGeneration("track-123");  // Check kafka would have been called

        assertThat(response.track().id()).isEqualTo("track-123");
        assertThat(response.track().topic()).isEqualTo("Kafka in Production");
        assertThat(response.syllabusStatus().ready()).isFalse();
    }

    @Test
    void createTrackRejectsInvalidTargetMinutes() {
        CreateTrackRequest request = new CreateTrackRequest(
            "Kafka",
            3,
            "08:00",
            "Asia/Singapore",
            List.of("web")
        );

        assertThatThrownBy(() -> trackService.createTrack("user-123", request))
            .isInstanceOf(InvalidTrackRequestException.class)
            .hasMessage("targetMinutes must be one of: 2, 5, 10");

        verify(trackRepository, never()).save(any(Track.class));
        verifyNoInteractions(syllabusGenerationProducer);
    }

    @Test
    void getSyllabusStatusReturnsReadyWhenSyllabusExists() {
        Track track = Track.builder()
            .id("track-123")
            .userId("user-123")
            .topic("Kafka")
            .targetMinutes(5)
            .status(TrackStatus.ACTIVE)
            .deliveryTime("08:00")
            .timezone("Asia/Singapore")
            .channels(List.of("web"))
            .nextDeliveryAt(Instant.parse("2026-06-28T00:00:00Z"))
            .syllabusPointer(0)
            .syllabusGenerationStatus(SyllabusGenerationStatus.GENERATING)
            .createdAt(Instant.parse("2026-06-27T00:00:00Z"))
            .build();

        Syllabus syllabus = Syllabus.builder()
            .id("syllabus-123")
            .trackId("track-123")
            .items(List.of())
            .version(1)
            .generatedAt(Instant.parse("2026-06-27T00:01:00Z"))
            .build();

        when(trackRepository.findById("track-123")).thenReturn(Optional.of(track));
        when(syllabusRepository.findByTrackId("track-123")).thenReturn(Optional.of(syllabus));

        SyllabusStatusResponse response =
            trackService.getSyllabusStatus("user-123", "track-123");

        assertThat(response.trackId()).isEqualTo("track-123");
        assertThat(response.status()).isEqualTo(SyllabusGenerationStatus.READY);
        assertThat(response.ready()).isTrue();
        assertThat(response.syllabusId()).isEqualTo("syllabus-123");
    }
}