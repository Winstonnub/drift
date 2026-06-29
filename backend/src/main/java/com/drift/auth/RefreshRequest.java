package com.drift.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(

    @NotBlank
    String refreshToken

) {
}