package com.drift.tracks;

import com.drift.kafka.SyllabusGenerationProducer;
import com.drift.syllabus.Syllabus;
import com.drift.syllabus.SyllabusItem;
import com.drift.syllabus.SyllabusItemResponse;
import com.drift.syllabus.SyllabusNotFoundException;
import com.drift.syllabus.SyllabusRepository;
import com.drift.syllabus.SyllabusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TrackService {

    private static final Set<Integer> ALLOWED_TARGET_MINUTES = Set.of(2, 5, 10);

    private static final Set<String> ALLOWED_CHANNELS = Set.of("web", "email");

    private final TrackRepository trackRepository;

    private final SyllabusRepository syllabusRepository;

    private final SyllabusGenerationProducer syllabusGenerationProducer;

    public CreateTrackResponse createTrack(String userId, CreateTrackRequest request) {
        validateTargetMinutes(request.targetMinutes());

        LocalTime deliveryTime = parseDeliveryTime(request.deliveryTime());
        ZoneId timezone = parseTimezone(request.timezone());
        List<String> channels = normalizeChannels(request.channels());

        Instant now = Instant.now();
        Instant nextDeliveryAt = calculateNextDeliveryAt(deliveryTime, timezone, now);

        Track track = Track.builder()
            .userId(userId)
            .topic(request.topic().trim())
            .targetMinutes(request.targetMinutes())
            .status(TrackStatus.ACTIVE)
            .deliveryTime(deliveryTime.toString())
            .timezone(timezone.getId())
            .channels(channels)
            .nextDeliveryAt(nextDeliveryAt)
            .syllabusPointer(0)
            .syllabusGenerationStatus(SyllabusGenerationStatus.GENERATING)
            .createdAt(now)
            .build();

        Track savedTrack = trackRepository.save(track);

        syllabusGenerationProducer.requestSyllabusGeneration(savedTrack.getId());

        return new CreateTrackResponse(
            toResponse(savedTrack),
            toSyllabusStatusResponse(savedTrack)
        );
    }

    public TrackResponse getTrack(String userId, String id) {
        Track track = findOwnedTrack(userId, id);

        return toResponse(track);
    }

    public List<TrackResponse> getTracks(String userId) {
    return trackRepository.findByUserId(userId).stream()
        .map(this::toResponse)
        .toList();
}

    public SyllabusResponse getSyllabus(String userId, String trackId) {
        Track track = findOwnedTrack(userId, trackId);

        Syllabus syllabus = syllabusRepository.findByTrackId(track.getId())
            .orElseThrow(() -> new SyllabusNotFoundException(trackId));

        return toSyllabusResponse(syllabus);
    }

    public SyllabusStatusResponse getSyllabusStatus(String userId, String trackId) {
        Track track = findOwnedTrack(userId, trackId);

        return toSyllabusStatusResponse(track);
    }

    private Track findOwnedTrack(String userId, String id) {
        Track track = trackRepository.findById(id)
            .orElseThrow(() -> new TrackNotFoundException(id));

        if (!track.getUserId().equals(userId)) {
            throw new TrackNotFoundException(id);
        }

        return track;
    }

    private void validateTargetMinutes(Integer targetMinutes) {
        if (!ALLOWED_TARGET_MINUTES.contains(targetMinutes)) {
            throw new InvalidTrackRequestException("targetMinutes must be one of: 2, 5, 10");
        }
    }

    private LocalTime parseDeliveryTime(String deliveryTime) {
        try {
            return LocalTime.parse(deliveryTime);
        } catch (DateTimeException exception) {
            throw new InvalidTrackRequestException("deliveryTime must use HH:mm format, for example 08:00");
        }
    }

    private ZoneId parseTimezone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            throw new InvalidTrackRequestException("timezone must be a valid timezone ID, for example Asia/Singapore");
        }
    }

    private List<String> normalizeChannels(List<String> channels) {
        List<String> normalizedChannels = channels.stream()
            .map(channel -> channel == null ? "" : channel.trim().toLowerCase())
            .distinct()
            .toList();

        boolean hasInvalidChannel = normalizedChannels.stream()
            .anyMatch(channel -> !ALLOWED_CHANNELS.contains(channel));

        if (hasInvalidChannel) {
            throw new InvalidTrackRequestException("channels must contain only: web, email");
        }

        return normalizedChannels;
    }

    private Instant calculateNextDeliveryAt(LocalTime deliveryTime, ZoneId timezone, Instant now) {
        ZonedDateTime nowInUserTimezone = now.atZone(timezone);

        ZonedDateTime candidateDeliveryTime = nowInUserTimezone
            .with(deliveryTime)
            .withSecond(0)
            .withNano(0);

        if (!candidateDeliveryTime.isAfter(nowInUserTimezone)) {
            candidateDeliveryTime = candidateDeliveryTime.plusDays(1);
        }

        return candidateDeliveryTime.toInstant();
    }

    private TrackResponse toResponse(Track track) {
        return new TrackResponse(
            track.getId(),
            track.getTopic(),
            track.getTargetMinutes(),
            track.getStatus(),
            track.getDeliveryTime(),
            track.getTimezone(),
            track.getChannels(),
            track.getNextDeliveryAt(),
            track.getSyllabusPointer(),
            track.getSyllabusGenerationStatus(),
            track.getCreatedAt()
        );
    }

    private SyllabusStatusResponse toSyllabusStatusResponse(Track track) {
        Optional<Syllabus> syllabus = syllabusRepository.findByTrackId(track.getId());

        if (syllabus.isPresent()) {
            return new SyllabusStatusResponse(
                track.getId(),
                SyllabusGenerationStatus.READY,
                true,
                syllabus.get().getId()
            );
        }

        SyllabusGenerationStatus status = track.getSyllabusGenerationStatus();

        if (status == null) {
            status = SyllabusGenerationStatus.GENERATING;
        }

        return new SyllabusStatusResponse(
            track.getId(),
            status,
            false,
            null
        );
    }

    private SyllabusResponse toSyllabusResponse(Syllabus syllabus) {
        return new SyllabusResponse(
            syllabus.getId(),
            syllabus.getTrackId(),
            syllabus.getItems().stream()
                .map(this::toSyllabusItemResponse)
                .toList(),
            syllabus.getVersion(),
            syllabus.getGeneratedAt()
        );
    }

    private SyllabusItemResponse toSyllabusItemResponse(SyllabusItem item) {
        return new SyllabusItemResponse(
            item.getIndex(),
            item.getTitle(),
            item.getSummary(),
            item.getPrerequisites(),
            item.getStatus()
        );
    }

}