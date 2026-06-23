
package com.drift.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data // Lombok annotation to generate getters, setters, toString, equals, and hashCode methods
@Builder // Lombok annotation to implement the builder pattern for this class
@NoArgsConstructor // Lombok annotation to generate a no-argument constructor
@AllArgsConstructor // Lombok annotation to generate a constructor with arguments for all fields
public class RefreshToken {// embedded obj for refreshing tokens, they are stored on user doc
   
    private String token; // refresh token string

    private Instant issuedAt; // when the refresh token was issued

    private Instant expiresAt; // when the refresh token will expire

    private Instant revokedAt; // Null if not revoked, otherwise when it was revoked

}