package com.demo.sso.service;

import com.demo.sso.service.MicrosoftTokenVerifier;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.demo.sso.config.MicrosoftAuthProperties;
import com.demo.sso.model.AuthFlow;
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
        verifier = new MicrosoftTokenVerifier(decoder, properties, new ProviderIdentityNormalizer());
    }

    @Test
    void verifyIdToken_returnsNormalizedIdentityForValidToken() {
        NormalizedIdentity identity = verifier.verifyIdToken("valid-token", "expected-nonce", AuthFlow.CLIENT_SIDE);

        assertEquals("user@example.com", identity.email());
        assertEquals("https://login.microsoftonline.com/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/v2.0|microsoft-subject",
            identity.providerUserId());
    }

    @Test
    void verifyIdToken_rejectsNonceMismatch() {
        assertThrows(IllegalArgumentException.class,
            () -> verifier.verifyIdToken("valid-token", "different-nonce", AuthFlow.CLIENT_SIDE));
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
        )), microsoftProperties(), new ProviderIdentityNormalizer());

        assertThrows(IllegalArgumentException.class,
            () -> verifier.verifyIdToken("valid-token", "expected-nonce", AuthFlow.CLIENT_SIDE));
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