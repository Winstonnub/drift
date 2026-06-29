package com.drift.feedback;

import java.time.Instant;

public record FeedbackResponse(

    String lessonId,

    FeedbackSignal signal,

    String freeText,

    Instant submittedAt

) {
}