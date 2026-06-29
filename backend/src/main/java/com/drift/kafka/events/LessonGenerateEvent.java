package com.drift.kafka.events;

import java.time.Instant;

public record LessonGenerateEvent(

    String trackId,

    Instant requestedAt,

    boolean manual

) {
}