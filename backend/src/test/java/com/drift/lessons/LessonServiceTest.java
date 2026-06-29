package com.drift.lessons;

import com.drift.kafka.LessonGenerationProducer;
import com.drift.tracks.InvalidTrackRequestException;
import com.drift.tracks.SyllabusGenerationStatus;
import com.drift.tracks.Track;
import com.drift.tracks.TrackNotFoundException;
import com.drift.tracks.TrackRepository;
import com.drift.tracks.TrackStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LessonServiceTest {

    private TrackRepository trackRepository;

    private LessonRepository lessonRepository;

    private LessonGenerationProducer lessonGenerationProducer;

    private LessonService lessonService;

    @BeforeEach
    void setUp() {
        trackRepository = mock(TrackRepository.class);
        lessonRepository = mock(LessonRepository.class);
        lessonGenerationProducer = mock(LessonGenerationProducer.class);
        lessonService = new LessonService(
            trackRepository,
            lessonRepository,
            lessonGenerationProducer
        );
    }

    @Test
    void requestLessonNowPublishesKafkaMessageWhenSyllabusIsReady() {
        Track track = readyTrack();
        when(trackRepository.findById("track-123")).thenReturn(Optional.of(track));

        lessonService.requestLessonNow("user-123", "track-123");

        verify(lessonGenerationProducer).requestLessonGeneration("track-123", true);
    }

    @Test
    void requestLessonNowRejectsTrackWhenSyllabusIsNotReady() {
        Track track = readyTrack();
        track.setSyllabusGenerationStatus(SyllabusGenerationStatus.GENERATING);

        when(trackRepository.findById("track-123")).thenReturn(Optional.of(track));

        assertThatThrownBy(() -> lessonService.requestLessonNow("user-123", "track-123"))
            .isInstanceOf(InvalidTrackRequestException.class)
            .hasMessage("Syllabus is not ready yet");

        verify(lessonGenerationProducer, never())
            .requestLessonGeneration(anyString(), anyBoolean());
    }

    @Test
    void getLessonsReturnsLessonResponses() {
        Track track = readyTrack();

        Lesson newerLesson = Lesson.builder()
            .id("lesson-new")
            .trackId("track-123")
            .userId("user-123")
            .syllabusItemIndex(1)
            .title("New lesson")
            .contentMarkdown("# New")
            .estimatedMinutes(5)
            .deliveredAt(Instant.parse("2026-06-28T10:00:00Z"))
            .build();

        Lesson olderLesson = Lesson.builder()
            .id("lesson-old")
            .trackId("track-123")
            .userId("user-123")
            .syllabusItemIndex(0)
            .title("Old lesson")
            .contentMarkdown("# Old")
            .estimatedMinutes(5)
            .deliveredAt(Instant.parse("2026-06-27T10:00:00Z"))
            .build();

        when(trackRepository.findById("track-123")).thenReturn(Optional.of(track));
        when(lessonRepository.findByTrackIdOrderByDeliveredAtDesc("track-123"))
            .thenReturn(List.of(newerLesson, olderLesson));

        List<LessonResponse> responses = lessonService.getLessons("user-123", "track-123");

        assertThat(responses)
            .extracting(LessonResponse::title)
            .containsExactly("New lesson", "Old lesson");
    }

    @Test
    void getLessonsHidesTracksOwnedByAnotherUser() {
        Track track = readyTrack();
        track.setUserId("someone-else");

        when(trackRepository.findById("track-123")).thenReturn(Optional.of(track));

        assertThatThrownBy(() -> lessonService.getLessons("user-123", "track-123"))
            .isInstanceOf(TrackNotFoundException.class);
    }

    private Track readyTrack() { // keeps the setup readable
        return Track.builder()
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
            .syllabusGenerationStatus(SyllabusGenerationStatus.READY)
            .createdAt(Instant.parse("2026-06-27T00:00:00Z"))
            .build();
    }
}