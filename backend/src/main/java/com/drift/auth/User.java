package com.drift.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "users") // map this class to MongoDB collection named "users". Each instance of this class is a document in that collection
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private String id; // _id field in MongoDB, unique identifier for this document

    @Indexed(unique = true)
    private String email; // email must be unique

    private String passwordHash;

    private String displayName; //

    private String timezone;

    private String defaultDeliveryTime;

    private List<String> defaultChannels;

    @Builder.Default // lombok annotation to set default value for this field when using builder pattern
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    private Instant createdAt;

    private Instant updatedAt;

}