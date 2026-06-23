// What we’re doing: Implementing register, login, and refresh.
package com.drift.auth;

import com.drift.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder; // injected from Security config

    private final JwtService jwtService;

    @Value("${app.jwt.refresh-token-expiration-days}")
    private long refreshTokenExpirationDays;

    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        Instant now = Instant.now();

        User user = User.builder()
            .email(normalizedEmail)
            .passwordHash(passwordEncoder.encode(request.password())) // hashes raw password
            .displayName(request.displayName().trim())
            .timezone("Asia/Singapore")
            .defaultDeliveryTime("08:00")
            .defaultChannels(List.of("web"))
            .createdAt(now)
            .updatedAt(now)
            .build();

        User savedUser = userRepository.save(user);

        return issueTokens(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        User user = userRepository.findByEmail(normalizedEmail)
            .orElseThrow(InvalidCredentialsException::new);

        boolean passwordMatches = passwordEncoder.matches( // compare raw login password with stored hash
            request.password(),
            user.getPasswordHash() 
        );

        if (!passwordMatches) {
            throw new InvalidCredentialsException();
        }

        return issueTokens(user); // creates an access token and refresh token for the user, and returns them in an AuthResponse
    }

    public AuthResponse refresh(RefreshRequest request) {
        User user = userRepository.findAll().stream()
            .filter(candidate -> hasActiveRefreshToken(candidate, request.refreshToken()))
            .findFirst()
            .orElseThrow(InvalidRefreshTokenException::new);

        revokeRefreshToken(user, request.refreshToken());

        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        RefreshToken refreshToken = createRefreshToken();

        user.getRefreshTokens().add(refreshToken);
        user.setUpdatedAt(Instant.now());

        User savedUser = userRepository.save(user);

        return new AuthResponse(
            accessToken,
            refreshToken.getToken(),
            "Bearer",
            savedUser.getId(),
            savedUser.getEmail(),
            savedUser.getDisplayName()
        );
    }

    private RefreshToken createRefreshToken() {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofDays(refreshTokenExpirationDays));

        return RefreshToken.builder()
            .token(generateSecureToken())
            .issuedAt(now)
            .expiresAt(expiresAt)
            .revokedAt(null)
            .build();
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[64];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private boolean hasActiveRefreshToken(User user, String refreshToken) {
        return user.getRefreshTokens().stream()
            .anyMatch(token ->
                token.getToken().equals(refreshToken)
                    && token.getRevokedAt() == null
                    && token.getExpiresAt().isAfter(Instant.now())
            );
    }

    private void revokeRefreshToken(User user, String refreshToken) {
        user.getRefreshTokens().stream()
            .filter(token -> token.getToken().equals(refreshToken))
            .findFirst()
            .ifPresent(token -> token.setRevokedAt(Instant.now()));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

}