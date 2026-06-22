package com.drift.tracks;

import com.fasterxml.jackson.annotation.JsonValue; // Jackson is JSON library for Spring Boot

public enum TrackStatus { // type with fixed set of possible values
    
    ACTIVE("active"),
    PAUSED("paused"),
    ARCHIVED("archived");

    private final String value;
    TrackStatus(String value) { // enum constructor
        this.value = value;
    }
    @JsonValue // serialize enum as its string value, not the enum name
    public String getValue() {
        return value;
    }


}
