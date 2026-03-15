package com.demo.sso.service.token;

import com.demo.sso.service.token.JwtTokenService;
import com.demo.sso.service.model.AuthenticatedUserIdentity;
import com.demo.sso.config.AuthRolloutProperties;
import com.demo.sso.model.AuthProvider;
import com.demo.sso.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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
        String token = jwtTokenService.generateLegacyToken("user@example.com", "google-123");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void getEmailFromToken_returnsCorrectEmail() {
        String token = jwtTokenService.generateLegacyToken("user@example.com", "google-123");
        String email = jwtTokenService.getEmailFromToken(token);
        assertEquals("user@example.com", email);
    }

    @Test
    void parseToken_containsGoogleIdClaim() {
        String token = jwtTokenService.generateLegacyToken("user@example.com", "google-123");
        var claims = jwtTokenService.parseToken(token);
        assertEquals("google-123", claims.get("googleId", String.class));
    }

    @Test
    void parseToken_containsIssuerAudienceAndJti() {
        String token = jwtTokenService.generateLegacyToken("user@example.com", "google-123");
        var claims = jwtTokenService.parseToken(token);
        assertEquals("sso-demo-backend", claims.getIssuer());
        assertTrue(claims.getAudience().contains("sso-demo-api"));
        assertNotNull(claims.getId());
        assertFalse(claims.getId().isBlank());
    }

    @Test
    void isTokenValid_returnsTrueForValidToken() {
        String token = jwtTokenService.generateLegacyToken("user@example.com", "google-123");
        assertTrue(jwtTokenService.isTokenValid(token));
    }

    @Test
    void isTokenValid_returnsFalseForGarbageToken() {
        assertFalse(jwtTokenService.isTokenValid("not.a.valid.jwt.token"));
    }

    @Test
    void isTokenValid_returnsFalseForTamperedToken() {
        String token = jwtTokenService.generateLegacyToken("user@example.com", "google-123");
        String tampered = token.substring(0, token.length() - 1) + "X";
        assertFalse(jwtTokenService.isTokenValid(tampered));
    }

    @Test
    void isTokenValid_returnsFalseForExpiredToken() {
        JwtTokenService expiredService = new JwtTokenService(
            "test-secret-key-that-is-at-least-32-characters-long-for-hmac", 0);
        String token = expiredService.generateLegacyToken("user@example.com", "google-123");
        assertFalse(expiredService.isTokenValid(token));
    }

    @Test
    void isTokenValid_returnsFalseForTokenSignedWithDifferentKey() {
        JwtTokenService otherService = new JwtTokenService(
            "another-secret-key-that-is-at-least-32-characters-long!!", 86400000);
        String tokenFromOther = otherService.generateLegacyToken("user@example.com", "google-123");
        assertFalse(jwtTokenService.isTokenValid(tokenFromOther));
    }

    @Test
    void constructor_rejectsShortSecret() {
        assertThrows(IllegalStateException.class,
            () -> new JwtTokenService("short", 86400000));
    }

    @Test
    void constructor_rejectsDefaultPlaceholder() {
        // Exact match "default" (padded to 32+ chars)
        assertThrows(IllegalStateException.class,
            () -> new JwtTokenService(
                "default                          ", 86400000));
    }

    @Test
    void constructor_allowsSecretContainingDefaultSubstring() {
        // Should NOT reject a random secret that happens to contain "default" as substring
        assertDoesNotThrow(
            () -> new JwtTokenService(
                "my-random-string-with-default-in-it-but-is-valid", 86400000));
    }

    @Test
    void generateToken_forUserInV2MintMode_usesUserIdSubjectAndProviderClaims() {
        JwtTokenService v2Service = new JwtTokenService(
            "test-secret-key-that-is-at-least-32-characters-long-for-hmac",
            86400000,
            rolloutProperties(AuthRolloutProperties.IdentityContractMode.COMPATIBILITY, AuthRolloutProperties.JwtMintMode.V2)
        );

        User user = new User();
        user.setId(42L);
        user.setEmail("user@example.com");
        user.setGoogleId("google-123");
        user.setProvider(AuthProvider.GOOGLE);
        user.setProviderUserId("google-123");

        String token = v2Service.generateToken(user);
        var claims = v2Service.parseToken(token);

        assertEquals("42", claims.getSubject());
        assertEquals(2, claims.get("ver", Integer.class));
        assertEquals("GOOGLE", claims.get("provider", String.class));
        assertEquals("google-123", claims.get("providerUserId", String.class));
        assertEquals("user@example.com", claims.get("email", String.class));
        assertNull(claims.get("googleId", String.class));
    }

    @Test
    void parseAuthenticatedUser_acceptsLegacyTokenDuringCompatibilityMode() {
        JwtTokenService compatibilityService = new JwtTokenService(
            "test-secret-key-that-is-at-least-32-characters-long-for-hmac",
            86400000,
            rolloutProperties(AuthRolloutProperties.IdentityContractMode.COMPATIBILITY, AuthRolloutProperties.JwtMintMode.LEGACY)
        );

        String token = compatibilityService.generateLegacyToken("user@example.com", "google-123");
        AuthenticatedUserIdentity identity = compatibilityService.parseAuthenticatedUser(token);

        assertTrue(identity.isLegacy());
        assertEquals("user@example.com", identity.email());
        assertNull(identity.userId());
    }

    @Test
    void isTokenValid_rejectsLegacyTokenWhenContractIsV2Only() {
        JwtTokenService legacyService = new JwtTokenService(
            "test-secret-key-that-is-at-least-32-characters-long-for-hmac",
            86400000,
            rolloutProperties(AuthRolloutProperties.IdentityContractMode.LEGACY_ONLY, AuthRolloutProperties.JwtMintMode.LEGACY)
        );
        JwtTokenService v2OnlyService = new JwtTokenService(
            "test-secret-key-that-is-at-least-32-characters-long-for-hmac",
            86400000,
            rolloutProperties(AuthRolloutProperties.IdentityContractMode.V2_ONLY, AuthRolloutProperties.JwtMintMode.V2)
        );

        String legacyToken = legacyService.generateLegacyToken("user@example.com", "google-123");

        assertFalse(v2OnlyService.isTokenValid(legacyToken));
        assertThrows(IllegalArgumentException.class, () -> v2OnlyService.parseAuthenticatedUser(legacyToken));
    }

    private static AuthRolloutProperties rolloutProperties(
            AuthRolloutProperties.IdentityContractMode identityContractMode,
            AuthRolloutProperties.JwtMintMode jwtMintMode) {
        AuthRolloutProperties properties = new AuthRolloutProperties();
        properties.setIdentityContractMode(identityContractMode);
        properties.setJwtMintMode(jwtMintMode);
        return properties;
    }
}
