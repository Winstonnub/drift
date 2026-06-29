package com.drift.tracks;

import java.time.Instant;
import java.util.List;

public record TrackResponse(

    String id,

    String topic,

    Integer targetMinutes,

    TrackStatus status,

    String deliveryTime,

    String timezone,

    List<String> channels,

    Instant nextDeliveryAt,

    Integer syllabusPointer,

    SyllabusGenerationStatus syllabusGenerationStatus,

    Instant createdAt

) {
}