package com.drift.tracks;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

@Service // mark this class as a Spring service component, so Spring will create an instance and manage its lifecycle
@RequiredArgsConstructor // Lombok annotation to generate a constructor with required arguments for final fields
public class TrackService {

    private static final String DEV_USER_ID = "dev-user";

    private static final Set<Integer> ALLOWED_TARGET_MINUTES = Set.of(2, 5, 10);

    private static final Set<String> ALLOWED_CHANNELS = Set.of("web", "email");

    private final TrackRepository trackRepository;

    public TrackResponse createTrack(CreateTrackRequest request) {
        validateTargetMinutes(request.targetMinutes());

        LocalTime deliveryTime = parseDeliveryTime(request.deliveryTime());
        ZoneId timezone = parseTimezone(request.timezone());
        List<String> channels = normalizeChannels(request.channels());

        Instant now = Instant.now();
        Instant nextDeliveryAt = calculateNextDeliveryAt(deliveryTime, timezone, now);

        Track track = Track.builder()
            .userId(DEV_USER_ID)
            .topic(request.topic().trim())
            .targetMinutes(request.targetMinutes())
            .status(TrackStatus.ACTIVE)
            .deliveryTime(deliveryTime.toString())
            .timezone(timezone.getId())
            .channels(channels)
            .nextDeliveryAt(nextDeliveryAt)
            .syllabusPointer(0)
            .createdAt(now)
            .build();

        Track savedTrack = trackRepository.save(track);

        return toResponse(savedTrack);
    }

    public TrackResponse getTrack(String id) {
        Track track = trackRepository.findById(id)
            .orElseThrow(() -> new TrackNotFoundException(id));

        return toResponse(track);
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
            track.getCreatedAt()
        );
    }

}