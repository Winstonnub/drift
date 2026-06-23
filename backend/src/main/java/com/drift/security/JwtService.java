package com.drift.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;

    private final Duration accessTokenExpiration;

    public JwtService(
        @Value("${app.jwt.secret}") String jwtSecret, // Reads app.jwt.secret from application.yml and injects it into this constructor parameter
        @Value("${app.jwt.access-token-expiration-minutes}") long accessTokenExpirationMinutes
    ) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)); // creates a signing key for HMAC-based JWTs using the provided secret string, converting it to bytes using UTF-8 encoding
        this.accessTokenExpiration = Duration.ofMinutes(accessTokenExpirationMinutes); // config into time duration
    }

    public String generateAccessToken(String userId, String email) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessTokenExpiration);

        return Jwts.builder() // Build a JWT token using the JJWT library
            .subject(userId) // KEY field to idenitfy the authenticated user
            .claim("email", email) // extra token metadata
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(secretKey) // sign the token. If someone edit the token, validation fails
            .compact(); // turns token into final string, sent to client
    }

    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

}