package com.demo.sso.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtConfigTest {

    private JwtConfig jwtConfig;

    @BeforeEach
    void setUp() {
        // 256-bit key (32+ chars required for HMAC-SHA256)
        String secret = "test-secret-key-that-is-at-least-32-characters-long-for-hmac";
        long expirationMs = 86400000; // 24 hours
        jwtConfig = new JwtConfig(secret, expirationMs);
    }

    @Test
    void generateToken_returnsNonEmptyString() {
        String token = jwtConfig.generateToken("user@example.com", "google-123");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void getEmailFromToken_returnsCorrectEmail() {
        String token = jwtConfig.generateToken("user@example.com", "google-123");
        String email = jwtConfig.getEmailFromToken(token);
        assertEquals("user@example.com", email);
    }

    @Test
    void parseToken_containsGoogleIdClaim() {
        String token = jwtConfig.generateToken("user@example.com", "google-123");
        var claims = jwtConfig.parseToken(token);
        assertEquals("google-123", claims.get("googleId", String.class));
    }

    @Test
    void isTokenValid_returnsTrueForValidToken() {
        String token = jwtConfig.generateToken("user@example.com", "google-123");
        assertTrue(jwtConfig.isTokenValid(token));
    }

    @Test
    void isTokenValid_returnsFalseForGarbageToken() {
        assertFalse(jwtConfig.isTokenValid("not.a.valid.jwt.token"));
    }

    @Test
    void isTokenValid_returnsFalseForTamperedToken() {
        String token = jwtConfig.generateToken("user@example.com", "google-123");
        // Tamper with the token by flipping a character in the signature
        String tampered = token.substring(0, token.length() - 1) + "X";
        assertFalse(jwtConfig.isTokenValid(tampered));
    }

    @Test
    void isTokenValid_returnsFalseForExpiredToken() {
        // Create JwtConfig with 0ms expiration (immediately expired)
        JwtConfig expiredConfig = new JwtConfig(
            "test-secret-key-that-is-at-least-32-characters-long-for-hmac", 0);
        String token = expiredConfig.generateToken("user@example.com", "google-123");
        assertFalse(expiredConfig.isTokenValid(token));
    }

    @Test
    void isTokenValid_returnsFalseForTokenSignedWithDifferentKey() {
        JwtConfig otherConfig = new JwtConfig(
            "another-secret-key-that-is-at-least-32-characters-long!!", 86400000);
        String tokenFromOther = otherConfig.generateToken("user@example.com", "google-123");
        assertFalse(jwtConfig.isTokenValid(tokenFromOther));
    }
}
