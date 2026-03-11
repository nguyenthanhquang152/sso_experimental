package com.demo.sso.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        String secret = "test-secret-key-that-is-at-least-32-characters-long-for-hmac";
        long expirationMs = 86400000; // 24 hours
        jwtTokenService = new JwtTokenService(secret, expirationMs);
    }

    @Test
    void generateToken_returnsNonEmptyString() {
        String token = jwtTokenService.generateToken("user@example.com", "google-123");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void getEmailFromToken_returnsCorrectEmail() {
        String token = jwtTokenService.generateToken("user@example.com", "google-123");
        String email = jwtTokenService.getEmailFromToken(token);
        assertEquals("user@example.com", email);
    }

    @Test
    void parseToken_containsGoogleIdClaim() {
        String token = jwtTokenService.generateToken("user@example.com", "google-123");
        var claims = jwtTokenService.parseToken(token);
        assertEquals("google-123", claims.get("googleId", String.class));
    }

    @Test
    void isTokenValid_returnsTrueForValidToken() {
        String token = jwtTokenService.generateToken("user@example.com", "google-123");
        assertTrue(jwtTokenService.isTokenValid(token));
    }

    @Test
    void isTokenValid_returnsFalseForGarbageToken() {
        assertFalse(jwtTokenService.isTokenValid("not.a.valid.jwt.token"));
    }

    @Test
    void isTokenValid_returnsFalseForTamperedToken() {
        String token = jwtTokenService.generateToken("user@example.com", "google-123");
        String tampered = token.substring(0, token.length() - 1) + "X";
        assertFalse(jwtTokenService.isTokenValid(tampered));
    }

    @Test
    void isTokenValid_returnsFalseForExpiredToken() {
        JwtTokenService expiredService = new JwtTokenService(
            "test-secret-key-that-is-at-least-32-characters-long-for-hmac", 0);
        String token = expiredService.generateToken("user@example.com", "google-123");
        assertFalse(expiredService.isTokenValid(token));
    }

    @Test
    void isTokenValid_returnsFalseForTokenSignedWithDifferentKey() {
        JwtTokenService otherService = new JwtTokenService(
            "another-secret-key-that-is-at-least-32-characters-long!!", 86400000);
        String tokenFromOther = otherService.generateToken("user@example.com", "google-123");
        assertFalse(jwtTokenService.isTokenValid(tokenFromOther));
    }

    @Test
    void constructor_rejectsShortSecret() {
        assertThrows(IllegalStateException.class,
            () -> new JwtTokenService("short", 86400000));
    }

    @Test
    void constructor_rejectsDefaultPlaceholder() {
        assertThrows(IllegalStateException.class,
            () -> new JwtTokenService(
                "default-dev-secret-change-me-in-production-please", 86400000));
    }
}
