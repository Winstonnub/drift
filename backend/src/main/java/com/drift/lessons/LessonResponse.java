package com.drift.lessons;

import com.drift.feedback.LessonFeedback;

import java.time.Instant;

public record LessonResponse(

    String id,

    String trackId,

    Integer syllabusItemIndex,

    String title,

    String contentMarkdown,

    Integer estimatedMinutes,

    Instant deliveredAt,

    Instant readAt,

    LessonFeedback feedback

) {
}