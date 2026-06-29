package com.drift.lessons;

import com.drift.feedback.LessonFeedback; // now Lesson uses feedback object from feedback package
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "lessons")
@CompoundIndex(name = "track_delivered_at_idx", def = "{'trackId': 1, 'deliveredAt': -1}")
@CompoundIndex(name = "track_syllabus_item_idx", def = "{'trackId': 1, 'syllabusItemIndex': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lesson {

    @Id
    private String id;

    @Indexed
    private String trackId;

    @Indexed
    private String userId;

    private Integer syllabusItemIndex;

    private String title;

    private String contentMarkdown;

    private Integer estimatedMinutes;

    private Instant deliveredAt;

    private Instant readAt;

    private LessonFeedback feedback;

}