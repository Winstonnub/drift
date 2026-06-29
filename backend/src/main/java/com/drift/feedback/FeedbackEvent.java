package com.drift.feedback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "feedback_events") // store these records in MongoDB feedback_events collection
@Data 
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackEvent {

    @Id
    private String id;

    @Indexed // cache
    private String userId;

    @Indexed
    private String trackId;

    @Indexed
    private String lessonId;

    private FeedbackSignal signal;

    private String freeText;

    private Instant createdAt;

}