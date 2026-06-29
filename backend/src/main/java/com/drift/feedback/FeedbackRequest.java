package com.drift.feedback;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FeedbackRequest(

    @NotNull
    FeedbackSignal signal,

    @Size(max = 1000)
    String freeText

) {
}