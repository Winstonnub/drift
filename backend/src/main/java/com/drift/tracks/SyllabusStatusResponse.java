package com.drift.tracks;

public record SyllabusStatusResponse(

    String trackId,

    SyllabusGenerationStatus status,

    boolean ready,

    String syllabusId

) {
}