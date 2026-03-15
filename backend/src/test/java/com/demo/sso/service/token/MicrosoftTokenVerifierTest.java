package com.demo.sso.service.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.demo.sso.config.properties.MicrosoftAuthProperties;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class MicrosoftTokenVerifierTest {

    private JwtDecoder decoder;
    private MicrosoftTokenVerifier verifier;

    @BeforeEach
    void setUp() {
        MicrosoftAuthProperties properties = new MicrosoftAuthProperties();
        properties.setClientId("test-microsoft-client-id");
        properties.setAuthority("https://login.microsoftonline.com/common/v2.0");
        properties.setScopes(List.of("openid", "profile", "email"));
        decoder = token -> validJwt();
        verifier = new MicrosoftTokenVerifier(decoder, properties);
    }

    @Test
    void verifyIdToken_returnsClaimsForValidToken() {
        MicrosoftIdTokenClaims claims = verifier.verifyIdToken("valid-token", "expected-nonce");

        assertEquals("user@example.com", claims.email());
        assertEquals("microsoft-subject", claims.sub());
        assertEquals("https://login.microsoftonline.com/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/v2.0", claims.iss());
    }

    @Test
    void verifyIdToken_rejectsNonceMismatch() {
        assertThrows(IllegalArgumentException.class,
            () -> verifier.verifyIdToken("valid-token", "different-nonce"));
    }

    @Test
    void verifyIdToken_rejectsUnexpectedAudience() {
        verifier = new MicrosoftTokenVerifier(token -> jwt(Map.of(
            "aud", "another-client-id",
            "iss", "https://login.microsoftonline.com/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/v2.0",
            "tid", "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
            "sub", "microsoft-subject",
            "ver", "2.0",
            "nonce", "expected-nonce",
            "preferred_username", "user@example.com"
        )), microsoftProperties());

        assertThrows(IllegalArgumentException.class,
            () -> verifier.verifyIdToken("valid-token", "expected-nonce"));
    }

    private MicrosoftAuthProperties microsoftProperties() {
        MicrosoftAuthProperties properties = new MicrosoftAuthProperties();
        properties.setClientId("test-microsoft-client-id");
        properties.setAuthority("https://login.microsoftonline.com/common/v2.0");
        properties.setScopes(List.of("openid", "profile", "email"));
        return properties;
    }

    private Jwt validJwt() {
        return jwt(Map.of(
            "aud", "test-microsoft-client-id",
            "iss", "https://login.microsoftonline.com/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/v2.0",
            "tid", "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
            "sub", "microsoft-subject",
            "ver", "2.0",
            "nonce", "expected-nonce",
            "email", "user@example.com",
            "name", "Microsoft User"
        ));
    }

    private Jwt jwt(Map<String, Object> claims) {
        return new Jwt(
            "token-value",
            Instant.now().minusSeconds(30),
            Instant.now().plusSeconds(300),
            Map.of("alg", "RS256", "kid", "kid-123"),
            claims
        );
    }
}