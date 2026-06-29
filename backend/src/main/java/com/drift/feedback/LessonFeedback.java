package com.drift.feedback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonFeedback {

    private FeedbackSignal signal;

    private String freeText;

    private Instant submittedAt;

}