package com.drift.lessons;

import com.drift.kafka.EmailDeliveryProducer;
import com.drift.kafka.KafkaTopics;
import com.drift.kafka.events.LessonGenerateEvent;
import com.drift.llm.LlmService;
import com.drift.syllabus.Syllabus;
import com.drift.syllabus.SyllabusItem;
import com.drift.syllabus.SyllabusItemStatus;
import com.drift.syllabus.SyllabusRepository;
import com.drift.tracks.Track;
import com.drift.tracks.TrackRepository;
import com.drift.tracks.TrackStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LessonGenerationListener {

    private static final Logger log = LoggerFactory.getLogger(LessonGenerationListener.class);

    private final TrackRepository trackRepository;

    private final SyllabusRepository syllabusRepository;

    private final LessonRepository lessonRepository;

    private final LlmService llmService;

    private final EmailDeliveryProducer emailDeliveryProducer;

    @KafkaListener(
        topics = KafkaTopics.LESSON_GENERATE,
        groupId = "drift-lesson-generator"
    )
    public void generateLesson(LessonGenerateEvent event) {
        Track track = trackRepository.findById(event.trackId())
            .orElseThrow(() -> new IllegalStateException("Track not found: " + event.trackId()));

        if (track.getStatus() != TrackStatus.ACTIVE) {
            log.info("Skipping lesson generation for inactive track {}", track.getId());
            return;
        }

        Syllabus syllabus = syllabusRepository.findByTrackId(track.getId())
            .orElseThrow(() -> new IllegalStateException("Syllabus not found for track: " + track.getId()));

        SyllabusItem syllabusItem = getCurrentSyllabusItem(track, syllabus);

        if (lessonRepository.findByTrackIdAndSyllabusItemIndex(track.getId(), syllabusItem.getIndex()).isPresent()) {
            log.info("Lesson already exists for track {} item {}", track.getId(), syllabusItem.getIndex());
            return;
        }

        log.info("Generating lesson for track {} item {}", track.getId(), syllabusItem.getIndex());

        String lessonMarkdown = llmService.generateLesson(track, syllabusItem);

        Lesson lesson = Lesson.builder()
            .trackId(track.getId())
            .userId(track.getUserId())
            .syllabusItemIndex(syllabusItem.getIndex())
            .title(syllabusItem.getTitle())
            .contentMarkdown(lessonMarkdown)
            .estimatedMinutes(track.getTargetMinutes())
            .deliveredAt(Instant.now())
            .readAt(null)
            .build();

        Lesson savedLesson = lessonRepository.save(lesson);

        syllabusItem.setStatus(SyllabusItemStatus.DELIVERED);
        syllabusRepository.save(syllabus);

        track.setSyllabusPointer(track.getSyllabusPointer() + 1);
        trackRepository.save(track);

        if (track.getChannels().contains("email")) {
            emailDeliveryProducer.requestEmailDelivery(savedLesson.getId());
        }

        log.info("Generated lesson {} for track {}", savedLesson.getId(), track.getId());
    }

    @KafkaListener(
        topics = KafkaTopics.LESSON_GENERATE_DLQ,
        groupId = "drift-lesson-dlq"
    )
    public void handleLessonGenerationFailure(LessonGenerateEvent event) {
        log.warn("Lesson generation failed and moved to DLQ for track {}", event.trackId());
    }

    private SyllabusItem getCurrentSyllabusItem(Track track, Syllabus syllabus) {
        Integer pointer = track.getSyllabusPointer();

        if (pointer == null) {
            pointer = 0;
        }

        int currentIndex = pointer;

        return syllabus.getItems().stream()
            .filter(item -> item.getIndex() == currentIndex)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No syllabus item " + currentIndex + " for track: " + track.getId()
            ));
    }

}