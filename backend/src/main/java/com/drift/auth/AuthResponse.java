package com.drift.auth;

public record AuthResponse(

    String accessToken,

    String refreshToken,

    String tokenType,

    String userId,

    String email,

    String displayName

) {
}