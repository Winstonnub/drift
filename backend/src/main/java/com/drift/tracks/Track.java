package com.drift.tracks;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor; // lombok generate boring java code like getters, setters, constructors, builders, etc. to reduce boilerplate code
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant; // precise UTC timestamp
import java.util.List;

@Document(collection = "tracks") // maps this Java class to MongoDB collection named "tracks". Each instance of this class is a document in that collection
@Data // lombok generates getters, setters, toString, equals, hashCode
@Builder // builder pattern
@NoArgsConstructor // empty constructor, Spring Data need when rebuilding obj from MongoDB document
@AllArgsConstructor // Create constructor for all fields
public class Track {

    @Id // _id field in MongoDB, unique identifier for this document
    private String id;

    @Indexed // ask MongoDB create an index for faster query on this field
    private String userId;

    private String topic; // thing user wants to learn

    private Integer targetMinutes; // mins

    private TrackStatus status;

    private String deliveryTime; // like "08:00", "20:00", user preferred time to receive the track content

    private String timezone; // Timezone ID

    private List<String> channels; // web, email, slack, etc.

    @Indexed
    private Instant nextDeliveryAt; // indexing early since we will query on this field to find tracks that are due for delivery

    private Integer syllabusPointer; // points to next syllabus item to deliver, 0-based index into syllabus array

    private SyllabusGenerationStatus syllabusGenerationStatus;

    private Instant createdAt;

}