package com.drift.feedback;

import com.drift.kafka.FeedbackProducer;
import com.drift.lessons.Lesson;
import com.drift.lessons.LessonRepository;
import com.drift.tracks.Track;
import com.drift.tracks.TrackNotFoundException;
import com.drift.tracks.TrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final TrackRepository trackRepository;

    private final LessonRepository lessonRepository;

    private final FeedbackProducer feedbackProducer;

    public FeedbackResponse submitFeedback(
        String userId,
        String trackId,
        String lessonId,
        FeedbackRequest request
    ) {
        Track track = findOwnedTrack(userId, trackId);

        Lesson lesson = lessonRepository.findById(lessonId)
            .orElseThrow(() -> new LessonNotFoundException(lessonId));

        if (!lesson.getTrackId().equals(track.getId())) {
            throw new LessonNotFoundException(lessonId);
        }

        Instant submittedAt = Instant.now();
        String normalizedFreeText = normalizeFreeText(request.freeText());

        LessonFeedback feedback = LessonFeedback.builder()
            .signal(request.signal())
            .freeText(normalizedFreeText)
            .submittedAt(submittedAt)
            .build();

        lesson.setFeedback(feedback);
        lessonRepository.save(lesson);

        feedbackProducer.publishFeedbackSubmitted(
            userId,
            track.getId(),
            lesson.getId(),
            request.signal(),
            normalizedFreeText,
            submittedAt
        );

        return new FeedbackResponse(
            lesson.getId(),
            request.signal(),
            normalizedFreeText,
            submittedAt
        );
    }

    private Track findOwnedTrack(String userId, String trackId) {
        Track track = trackRepository.findById(trackId)
            .orElseThrow(() -> new TrackNotFoundException(trackId));

        if (!track.getUserId().equals(userId)) {
            throw new TrackNotFoundException(trackId);
        }

        return track;
    }

    private String normalizeFreeText(String freeText) {
        if (freeText == null || freeText.isBlank()) {
            return null;
        }

        return freeText.trim();
    }

}