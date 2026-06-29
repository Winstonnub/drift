package com.drift.syllabus;

import com.drift.kafka.KafkaTopics;
import com.drift.kafka.events.SyllabusGenerateEvent;
import com.drift.llm.LlmService;
import com.drift.tracks.SyllabusGenerationStatus;
import com.drift.tracks.Track;
import com.drift.tracks.TrackRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SyllabusGenerationListener {

    private static final Logger log = LoggerFactory.getLogger(SyllabusGenerationListener.class);

    private final TrackRepository trackRepository;

    private final SyllabusRepository syllabusRepository;

    private final LlmService llmService;

    @KafkaListener( // this method runs when a Kafka message arrives
        topics = KafkaTopics.SYLLABUS_GENERATE,
        groupId = "drift-syllabus-generator"
    )
    public void generateSyllabus(SyllabusGenerateEvent event) {
        try {
            generateSyllabusInternal(event);
        } catch (RuntimeException exception) {
            log.error("Syllabus generation failed for track {}", event.trackId(), exception);
            throw exception;
        }
    }

    private void generateSyllabusInternal(SyllabusGenerateEvent event) {
        Track track = trackRepository.findById(event.trackId())
            .orElseThrow(() -> new IllegalStateException("Track not found: " + event.trackId()));

        if (syllabusRepository.findByTrackId(track.getId()).isPresent()) {
            markTrackReady(track); // idempotent: if syllabus already exists, just mark track as ready
            return;
        }

        log.info("Generating syllabus for track {}", track.getId());

        List<SyllabusItem> syllabusItems = llmService.generateSyllabus(track.getTopic());

        Syllabus syllabus = Syllabus.builder()
            .trackId(track.getId())
            .items(syllabusItems)
            .version(1)
            .generatedAt(Instant.now())
            .build();

        syllabusRepository.save(syllabus);

        markTrackReady(track);

        log.info("Generated syllabus for track {}", track.getId());
    }

    @KafkaListener(
        topics = KafkaTopics.SYLLABUS_GENERATE_DLQ, // DefaultErrorHandler will send failed messages to this topic
        groupId = "drift-syllabus-dlq"
    )
    public void markSyllabusGenerationFailed(SyllabusGenerateEvent event) {
        log.warn("Syllabus generation failed and moved to DLQ for track {}", event.trackId());

        trackRepository.findById(event.trackId())
            .ifPresent(track -> {
                track.setSyllabusGenerationStatus(SyllabusGenerationStatus.FAILED);
                trackRepository.save(track);
            });
    }

    private void markTrackReady(Track track) {
        track.setSyllabusGenerationStatus(SyllabusGenerationStatus.READY);
        trackRepository.save(track);
    }

}
