package com.drift.syllabus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "syllabi")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Syllabus {

    @Id
    private String id;

    @Indexed(unique = true)
    private String trackId;

    private List<SyllabusItem> items;

    private Integer version;

    private Instant generatedAt;

}