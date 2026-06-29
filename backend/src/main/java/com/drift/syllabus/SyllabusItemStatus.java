package com.drift.syllabus;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SyllabusItemStatus {

    PENDING("pending"), // items start pending
    DELIVERED("delivered"), // lesson generated and delivered to user
    SKIPPED("skipped"); // feedback can skip ahead

    private final String value;

    SyllabusItemStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

}