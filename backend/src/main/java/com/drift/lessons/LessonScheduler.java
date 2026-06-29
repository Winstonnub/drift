package com.drift.lessons;

import com.drift.kafka.LessonGenerationProducer;
import com.drift.tracks.SyllabusGenerationStatus;
import com.drift.tracks.Track;
import com.drift.tracks.TrackRepository;
import com.drift.tracks.TrackStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonScheduler {

    private static final Logger log = LoggerFactory.getLogger(LessonScheduler.class);

    private final TrackRepository trackRepository;

    private final LessonGenerationProducer lessonGenerationProducer;

    @Scheduled(cron = "0 */5 * * * *")// spring's cron format
    public void enqueueDueLessons() {
        Instant now = Instant.now();

        List<Track> dueTracks = trackRepository
            .findByStatusAndSyllabusGenerationStatusAndNextDeliveryAtLessThanEqual( // only ready tracks get scheduled
                TrackStatus.ACTIVE,
                SyllabusGenerationStatus.READY,
                now
            );

        for (Track track : dueTracks) {
            lessonGenerationProducer.requestLessonGeneration(track.getId(), false);

            track.setNextDeliveryAt(calculateNextDeliveryAt(track, now));
            trackRepository.save(track);

            log.info("Queued lesson generation for track {}", track.getId());
        }
    }

    private Instant calculateNextDeliveryAt(Track track, Instant now) {
        try {
            LocalTime deliveryTime = LocalTime.parse(track.getDeliveryTime());
            ZoneId timezone = ZoneId.of(track.getTimezone());

            ZonedDateTime nowInUserTimezone = now.atZone(timezone);

            ZonedDateTime candidateDeliveryTime = nowInUserTimezone
                .with(deliveryTime)
                .withSecond(0)
                .withNano(0);

            while (!candidateDeliveryTime.isAfter(nowInUserTimezone)) {
                candidateDeliveryTime = candidateDeliveryTime.plusDays(1);
            }

            return candidateDeliveryTime.toInstant();
        } catch (DateTimeException exception) {
            log.warn("Invalid delivery schedule for track {}", track.getId(), exception);
            return now.plusSeconds(24 * 60 * 60);
        }
    }

}