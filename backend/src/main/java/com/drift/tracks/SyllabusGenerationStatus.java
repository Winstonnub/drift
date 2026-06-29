package com.drift.tracks;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SyllabusGenerationStatus {

    GENERATING("generating"),

    READY("ready"),

    FAILED("failed");

    private final String value;

    SyllabusGenerationStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

}