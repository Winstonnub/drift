package com.drift.syllabus;

import java.time.Instant;
import java.util.List;

public record SyllabusResponse(

    String id,

    String trackId,

    List<SyllabusItemResponse> items,

    Integer version,

    Instant generatedAt

) {
}