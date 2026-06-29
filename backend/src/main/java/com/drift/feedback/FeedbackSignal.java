package com.drift.feedback;

import com.fasterxml.jackson.annotation.JsonValue;

public enum FeedbackSignal {

    GOT_IT("got_it"),

    GO_DEEPER("go_deeper"),

    TOO_BASIC("too_basic"),

    CONFUSED("confused"),

    SKIP_AHEAD("skip_ahead");

    private final String value;

    FeedbackSignal(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

}