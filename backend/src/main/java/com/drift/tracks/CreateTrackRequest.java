package com.drift.tracks;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
// Bean Validation annotations for request validation, Spring use them to rej bad JSON
import java.util.List;

public record CreateTrackRequest( // records are immutable

    @NotBlank
    String topic,

    @NotNull
    Integer targetMinutes,

    @NotBlank
    String deliveryTime,

    @NotBlank
    String timezone,

    @NotEmpty
    List<String> channels

) {
}