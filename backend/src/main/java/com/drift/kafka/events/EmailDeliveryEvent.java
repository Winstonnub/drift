package com.drift.kafka.events;

import java.time.Instant;

public record EmailDeliveryEvent(

    String lessonId,

    Instant requestedAt

) {
}