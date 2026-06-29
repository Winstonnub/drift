package com.drift.feedback;

import com.drift.kafka.FeedbackProducer;
import com.drift.lessons.Lesson;
import com.drift.lessons.LessonRepository;
import com.drift.tracks.Track;
import com.drift.tracks.TrackRepository;
import com.drift.tracks.TrackStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedbackServiceTest {

    private TrackRepository trackRepository;

    private LessonRepository lessonRepository;

    private FeedbackProducer feedbackProducer;

    private FeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        trackRepository = mock(TrackRepository.class);
        lessonRepository = mock(LessonRepository.class);
        feedbackProducer = mock(FeedbackProducer.class);
        feedbackService = new FeedbackService(
            trackRepository,
            lessonRepository,
            feedbackProducer
        );
    }

    @Test
    void submitFeedbackStoresFeedbackAndPublishesEvent() {
        Track track = track();
        Lesson lesson = lesson("lesson-123", "track-123");

        when(trackRepository.findById("track-123")).thenReturn(Optional.of(track));
        when(lessonRepository.findById("lesson-123")).thenReturn(Optional.of(lesson));
        when(lessonRepository.save(lesson)).thenReturn(lesson);

        FeedbackRequest request = new FeedbackRequest(
            FeedbackSignal.GO_DEEPER,
            "  Give me a trading example  "
        );

        FeedbackResponse response = feedbackService.submitFeedback(
            "user-123",
            "track-123",
            "lesson-123",
            request
        );

        ArgumentCaptor<Lesson> lessonCaptor = ArgumentCaptor.forClass(Lesson.class);
        verify(lessonRepository).save(lessonCaptor.capture());

        Lesson savedLesson = lessonCaptor.getValue();

        assertThat(savedLesson.getFeedback()).isNotNull();
        assertThat(savedLesson.getFeedback().getSignal()).isEqualTo(FeedbackSignal.GO_DEEPER);
        assertThat(savedLesson.getFeedback().getFreeText())
            .isEqualTo("Give me a trading example");
        assertThat(savedLesson.getFeedback().getSubmittedAt()).isNotNull();

        verify(feedbackProducer).publishFeedbackSubmitted(
            eq("user-123"),
            eq("track-123"),
            eq("lesson-123"),
            eq(FeedbackSignal.GO_DEEPER),
            eq("Give me a trading example"),
            eq(response.submittedAt())
        );

        assertThat(response.lessonId()).isEqualTo("lesson-123");
        assertThat(response.signal()).isEqualTo(FeedbackSignal.GO_DEEPER);
        assertThat(response.freeText()).isEqualTo("Give me a trading example");
    }

    @Test
    void submitFeedbackRejectsLessonFromDifferentTrack() {
        Track track = track();
        Lesson lesson = lesson("lesson-123", "other-track");

        when(trackRepository.findById("track-123")).thenReturn(Optional.of(track));
        when(lessonRepository.findById("lesson-123")).thenReturn(Optional.of(lesson));

        FeedbackRequest request = new FeedbackRequest(FeedbackSignal.CONFUSED, null);

        assertThatThrownBy(() -> feedbackService.submitFeedback(
            "user-123",
            "track-123",
            "lesson-123",
            request
        )).isInstanceOf(LessonNotFoundException.class);
    }

    private Track track() {
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
            .createdAt(Instant.parse("2026-06-27T00:00:00Z"))
            .build();
    }

    private Lesson lesson(String lessonId, String trackId) {
        return Lesson.builder()
            .id(lessonId)
            .trackId(trackId)
            .userId("user-123")
            .syllabusItemIndex(0)
            .title("What is Kafka?")
            .contentMarkdown("# What is Kafka?")
            .estimatedMinutes(5)
            .deliveredAt(Instant.parse("2026-06-28T01:00:00Z"))
            .build();
    }
}