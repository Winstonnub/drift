package com.drift.feedback;

import com.drift.kafka.KafkaTopics;
import com.drift.kafka.events.FeedbackSubmittedEvent;
import com.drift.syllabus.Syllabus;
import com.drift.syllabus.SyllabusItem;
import com.drift.syllabus.SyllabusItemStatus;
import com.drift.syllabus.SyllabusRepository;
import com.drift.tracks.Track;
import com.drift.tracks.TrackRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackProcessingListener {

    private static final Logger log = LoggerFactory.getLogger(FeedbackProcessingListener.class);

    private final FeedbackEventRepository feedbackEventRepository;

    private final TrackRepository trackRepository;

    private final SyllabusRepository syllabusRepository;

    @KafkaListener(
        topics = KafkaTopics.FEEDBACK_SUBMITTED,
        groupId = "drift-feedback-processor"
    )
    public void processFeedback(FeedbackSubmittedEvent event) {
        saveFeedbackEvent(event);

        Track track = trackRepository.findById(event.trackId())
            .orElseThrow(() -> new IllegalStateException("Track not found: " + event.trackId()));

        Syllabus syllabus = syllabusRepository.findByTrackId(track.getId())
            .orElseThrow(() -> new IllegalStateException("Syllabus not found for track: " + track.getId()));

        switch (event.signal()) {
            case GOT_IT -> log.info("Feedback got_it for track {}; continuing normally", track.getId());
            case GO_DEEPER -> insertDetour(track, syllabus, event, "Deeper dive");
            case CONFUSED -> insertDetour(track, syllabus, event, "Simpler explanation");
            case TOO_BASIC, SKIP_AHEAD -> skipNextItem(track, syllabus);
        }
    }

    @KafkaListener(
        topics = KafkaTopics.FEEDBACK_SUBMITTED_DLQ,
        groupId = "drift-feedback-dlq"
    )
    public void handleFeedbackProcessingFailure(FeedbackSubmittedEvent event) {
        log.warn("Feedback processing failed and moved to DLQ for lesson {}", event.lessonId());
    }

    private void saveFeedbackEvent(FeedbackSubmittedEvent event) {
        FeedbackEvent feedbackEvent = FeedbackEvent.builder()
            .userId(event.userId())
            .trackId(event.trackId())
            .lessonId(event.lessonId())
            .signal(event.signal())
            .freeText(event.freeText())
            .createdAt(event.submittedAt())
            .build();

        feedbackEventRepository.save(feedbackEvent);
    }

    private void insertDetour(
        Track track,
        Syllabus syllabus,
        FeedbackSubmittedEvent event,
        String titlePrefix
    ) {
        int insertionIndex = safePointer(track);

        List<SyllabusItem> updatedItems = new ArrayList<>(syllabus.getItems());

        for (SyllabusItem item : updatedItems) {
            if (item.getIndex() >= insertionIndex) {
                item.setIndex(item.getIndex() + 1);
            }
        }

        SyllabusItem detourItem = SyllabusItem.builder()
            .index(insertionIndex)
            .title(titlePrefix + ": " + buildDetourTitle(event))
            .summary(buildDetourSummary(event))
            .prerequisites(List.of())
            .status(SyllabusItemStatus.PENDING)
            .build();

        updatedItems.add(detourItem);

        syllabus.setItems(updatedItems.stream()
            .sorted(Comparator.comparing(SyllabusItem::getIndex))
            .toList());
        syllabus.setVersion(syllabus.getVersion() + 1);

        syllabusRepository.save(syllabus);

        log.info("Inserted feedback detour at index {} for track {}", insertionIndex, track.getId());
    }

    private void skipNextItem(Track track, Syllabus syllabus) {
        int currentPointer = safePointer(track);

        syllabus.getItems().stream()
            .filter(item -> item.getIndex() == currentPointer)
            .findFirst()
            .ifPresent(item -> item.setStatus(SyllabusItemStatus.SKIPPED));

        track.setSyllabusPointer(currentPointer + 1);

        syllabusRepository.save(syllabus);
        trackRepository.save(track);

        log.info("Skipped syllabus item {} for track {}", currentPointer, track.getId());
    }

    private int safePointer(Track track) {
        if (track.getSyllabusPointer() == null) {
            return 0;
        }

        return track.getSyllabusPointer();
    }

    private String buildDetourTitle(FeedbackSubmittedEvent event) {
        if (event.freeText() == null || event.freeText().isBlank()) {
            return "follow-up lesson";
        }

        return event.freeText().trim();
    }

    private String buildDetourSummary(FeedbackSubmittedEvent event) {
        if (event.signal() == FeedbackSignal.CONFUSED) {
            return "Revisit the previous concept with a simpler explanation and a concrete example.";
        }

        return "Explore the previous concept in more depth using the learner's feedback.";
    }

}