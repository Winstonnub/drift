package com.drift.kafka.events;

import com.drift.feedback.FeedbackSignal;

import java.time.Instant;

public record FeedbackSubmittedEvent(

    String userId,

    String trackId,

    String lessonId,

    FeedbackSignal signal,

    String freeText,

    Instant submittedAt

) {
}

// Plain english: User X gave lesson Y on track Z this feedback