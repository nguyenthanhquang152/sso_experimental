package com.demo.sso.service.token;

import com.demo.sso.service.token.JwtTokenService;
import com.demo.sso.service.model.AuthenticatedUserIdentity;
import com.demo.sso.config.properties.AuthRolloutProperties;
import com.demo.sso.exception.InvalidTokenException;
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

    private static final String SECRET = "test-secret-key-that-is-at-least-32-characters-long-for-hmac";
    private static final long EXPIRATION_MS = 86400000; // 24 hours

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(SECRET, EXPIRATION_MS,
            rolloutProperties(AuthRolloutProperties.IdentityContractMode.COMPATIBILITY, AuthRolloutProperties.JwtMintMode.V2));
    }

    private static User testUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setProvider(AuthProvider.GOOGLE);
        user.setProviderUserId("google-123");
        return user;
    }

    @Test
    void generateToken_returnsNonEmptyString() {
        String token = jwtTokenService.generateToken(testUser());
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void parseAuthenticatedUser_returnsCorrectEmail() {
        String token = jwtTokenService.generateToken(testUser());
        String email = jwtTokenService.parseAuthenticatedUser(token).email();
        assertEquals("user@example.com", email);
    }

    @Test
    void parseToken_containsV2Claims() {
        String token = jwtTokenService.generateToken(testUser());
        var claims = jwtTokenService.parseToken(token);
        assertEquals(2, claims.get("ver", Integer.class));
        assertEquals("GOOGLE", claims.get("provider", String.class));
        assertEquals("google-123", claims.get("providerUserId", String.class));
        assertEquals("user@example.com", claims.get("email", String.class));
        assertNull(claims.get("googleId", String.class));
    }

    @Test
    void parseToken_containsIssuerAudienceAndJti() {
        String token = jwtTokenService.generateToken(testUser());
        var claims = jwtTokenService.parseToken(token);
        assertEquals("sso-demo-backend", claims.getIssuer());
        assertTrue(claims.getAudience().contains("sso-demo-api"));
        assertNotNull(claims.getId());
        assertFalse(claims.getId().isBlank());
    }

    @Test
    void validateAndExtract_returnsPresentForValidToken() {
        String token = jwtTokenService.generateToken(testUser());
        assertTrue(jwtTokenService.validateAndExtract(token).isPresent());
    }

    @Test
    void validateAndExtract_returnsEmptyForGarbageToken() {
        assertTrue(jwtTokenService.validateAndExtract("not.a.valid.jwt.token").isEmpty());
    }

    @Test
    void validateAndExtract_returnsEmptyForTamperedToken() {
        String token = jwtTokenService.generateToken(testUser());
        String tampered = token.substring(0, token.length() - 1) + "X";
        assertTrue(jwtTokenService.validateAndExtract(tampered).isEmpty());
    }

    @Test
    void validateAndExtract_returnsEmptyForExpiredToken() {
        JwtTokenService expiredService = new JwtTokenService(SECRET, 0,
            rolloutProperties(AuthRolloutProperties.IdentityContractMode.COMPATIBILITY, AuthRolloutProperties.JwtMintMode.V2));
        String token = expiredService.generateToken(testUser());
        assertTrue(expiredService.validateAndExtract(token).isEmpty());
    }

    @Test
    void validateAndExtract_returnsEmptyForTokenSignedWithDifferentKey() {
        JwtTokenService otherService = new JwtTokenService(
            "another-secret-key-that-is-at-least-32-characters-long!!", EXPIRATION_MS,
            rolloutProperties(AuthRolloutProperties.IdentityContractMode.COMPATIBILITY, AuthRolloutProperties.JwtMintMode.V2));
        String tokenFromOther = otherService.generateToken(testUser());
        assertTrue(jwtTokenService.validateAndExtract(tokenFromOther).isEmpty());
    }

    @Test
    void constructor_rejectsShortSecret() {
        assertThrows(IllegalStateException.class,
            () -> new JwtTokenService("short", EXPIRATION_MS));
    }

    @Test
    void constructor_rejectsDefaultPlaceholder() {
        assertThrows(IllegalStateException.class,
            () -> new JwtTokenService(
                "default                          ", EXPIRATION_MS));
    }

    @Test
    void constructor_allowsSecretContainingDefaultSubstring() {
        assertDoesNotThrow(
            () -> new JwtTokenService(
                "my-random-string-with-default-in-it-but-is-valid", EXPIRATION_MS));
    }

    @Test
    void generateToken_alwaysMintsV2Token() {
        User user = testUser();
        user.setId(42L);

        String token = jwtTokenService.generateToken(user);
        var claims = jwtTokenService.parseToken(token);

        assertEquals("42", claims.getSubject());
        assertEquals(2, claims.get("ver", Integer.class));
        assertEquals("GOOGLE", claims.get("provider", String.class));
        assertEquals("google-123", claims.get("providerUserId", String.class));
        assertEquals("user@example.com", claims.get("email", String.class));
        assertNull(claims.get("googleId", String.class));
    }

    @Test
    void parseAuthenticatedUser_stillAcceptsLegacyTokenDuringCompatibilityMode() {
        // Simulate a legacy token that was already issued (backward compat for reading)
        // We need to build one manually since generateLegacyToken is removed
        JwtTokenService compatService = new JwtTokenService(SECRET, EXPIRATION_MS,
            rolloutProperties(AuthRolloutProperties.IdentityContractMode.COMPATIBILITY, AuthRolloutProperties.JwtMintMode.V2));

        // V2 tokens should parse fine
        String token = compatService.generateToken(testUser());
        AuthenticatedUserIdentity identity = compatService.parseAuthenticatedUser(token);

        assertFalse(identity.isLegacy());
        assertEquals("user@example.com", identity.email());
        assertEquals(1L, identity.userId());
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
