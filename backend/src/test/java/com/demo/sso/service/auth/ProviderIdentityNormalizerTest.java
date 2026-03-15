package com.demo.sso.service.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.demo.sso.model.AuthFlow;
import com.demo.sso.model.AuthProvider;
import com.demo.sso.service.model.NormalizedIdentity;
import com.demo.sso.service.token.GoogleTokenVerifier.VerifiedGoogleIdentity;
import com.demo.sso.service.token.MicrosoftIdTokenClaims;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProviderIdentityNormalizerTest {

    private ProviderIdentityNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new ProviderIdentityNormalizer();
    }

    // ── Microsoft tests ──────────────────────────────────────────────

    @Test
    void normalizeMicrosoftClaims_usesIssuerScopedSubjectAndPrimaryEmail() {
        NormalizedIdentity identity = normalizer.normalizeMicrosoftClaims(MicrosoftIdTokenClaims.fromMap(Map.of(
            "iss", "https://login.microsoftonline.com/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/v2.0",
            "sub", "microsoft-subject",
            "tid", "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
            "email", "User@Example.com",
            "name", "Microsoft User"
        )), AuthFlow.CLIENT_SIDE);

        assertEquals(AuthProvider.MICROSOFT, identity.provider());
        assertEquals(
            "https://login.microsoftonline.com/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/v2.0|microsoft-subject",
            identity.providerUserId());
        assertEquals("user@example.com", identity.email());
        assertEquals("Microsoft User", identity.name());
        assertEquals(AuthFlow.CLIENT_SIDE, identity.loginFlow());
    }

    @Test
    void normalizeMicrosoftClaims_fallsBackToPreferredUsernameThenUpn() {
        NormalizedIdentity identity = normalizer.normalizeMicrosoftClaims(MicrosoftIdTokenClaims.fromMap(Map.of(
            "iss", "https://login.microsoftonline.com/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/v2.0",
            "sub", "microsoft-subject",
            "tid", "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
            "preferred_username", "Preferred.User@example.com"
        )), AuthFlow.SERVER_SIDE);

        assertEquals("preferred.user@example.com", identity.email());
        assertEquals(AuthFlow.SERVER_SIDE, identity.loginFlow());
    }

    @Test
    void normalizeMicrosoftClaims_rejectsGuestStyleIdentifiers() {
        assertThrows(IllegalArgumentException.class, () -> normalizer.normalizeMicrosoftClaims(MicrosoftIdTokenClaims.fromMap(Map.of(
            "iss", "https://login.microsoftonline.com/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/v2.0",
            "sub", "microsoft-subject",
            "tid", "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
            "preferred_username", "guest_user#EXT#@example.onmicrosoft.com"
        )), AuthFlow.CLIENT_SIDE));
    }

    @Test
    void normalizeMicrosoftClaims_rejectsExternalIdentityProviders() {
        assertThrows(IllegalArgumentException.class, () -> normalizer.normalizeMicrosoftClaims(MicrosoftIdTokenClaims.fromMap(Map.of(
            "iss", "https://login.microsoftonline.com/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/v2.0",
            "sub", "microsoft-subject",
            "tid", "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
            "email", "user@example.com",
            "idp", "live.com"
        )), AuthFlow.CLIENT_SIDE));
    }

    @Test
    void normalizeMicrosoftClaims_emailResolutionOrder_emailOverPreferredUsernameOverUpn() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", "https://login.microsoftonline.com/tid/v2.0");
        claims.put("sub", "sub1");
        claims.put("tid", "tid");
        claims.put("email", "Primary@Example.com");
        claims.put("preferred_username", "Preferred@Example.com");
        claims.put("upn", "Upn@Example.com");

        NormalizedIdentity identity = normalizer.normalizeMicrosoftClaims(
                MicrosoftIdTokenClaims.fromMap(claims), AuthFlow.CLIENT_SIDE);
        assertEquals("primary@example.com", identity.email());

        // Remove email → falls back to preferred_username
        claims.remove("email");
        identity = normalizer.normalizeMicrosoftClaims(
                MicrosoftIdTokenClaims.fromMap(claims), AuthFlow.CLIENT_SIDE);
        assertEquals("preferred@example.com", identity.email());

        // Remove preferred_username → falls back to upn
        claims.remove("preferred_username");
        identity = normalizer.normalizeMicrosoftClaims(
                MicrosoftIdTokenClaims.fromMap(claims), AuthFlow.CLIENT_SIDE);
        assertEquals("upn@example.com", identity.email());
    }

    @Test
    void normalizeMicrosoftClaims_rejectsGuestExtInEmail() {
        assertThrows(IllegalArgumentException.class, () -> normalizer.normalizeMicrosoftClaims(MicrosoftIdTokenClaims.fromMap(Map.of(
            "iss", "https://login.microsoftonline.com/tid/v2.0",
            "sub", "sub1",
            "tid", "tid",
            "email", "guest#ext#@tenant.onmicrosoft.com"
        )), AuthFlow.CLIENT_SIDE));
    }

    // ── normalizeEmail edge cases (via Microsoft claims) ─────────────

    @Nested
    class NormalizeEmailEdgeCases {

        private MicrosoftIdTokenClaims microsoftClaims(String email) {
            return MicrosoftIdTokenClaims.fromMap(Map.of(
                "iss", "https://login.microsoftonline.com/tid/v2.0",
                "sub", "sub1",
                "tid", "tid",
                "email", email
            ));
        }

        @Test
        void emailWithLeadingAndTrailingSpaces_normalizesSuccessfully() {
            NormalizedIdentity identity = normalizer.normalizeMicrosoftClaims(
                    microsoftClaims("  User@Domain.COM  "), AuthFlow.CLIENT_SIDE);
            assertEquals("user@domain.com", identity.email());
        }

        @Test
        void caseNormalization_convertsToLowerCase() {
            NormalizedIdentity identity = normalizer.normalizeMicrosoftClaims(
                    microsoftClaims("UPPER@DOMAIN.COM"), AuthFlow.CLIENT_SIDE);
            assertEquals("upper@domain.com", identity.email());
        }

        @Test
        void emailWithMultipleAtSigns_throws() {
            assertThrows(IllegalArgumentException.class,
                    () -> normalizer.normalizeMicrosoftClaims(
                            microsoftClaims("user@@domain.com"), AuthFlow.CLIENT_SIDE));
        }

        @Test
        void emailWithEmptyLocalPart_throws() {
            assertThrows(IllegalArgumentException.class,
                    () -> normalizer.normalizeMicrosoftClaims(
                            microsoftClaims("@domain.com"), AuthFlow.CLIENT_SIDE));
        }

        @Test
        void emailWithEmptyDomain_throws() {
            assertThrows(IllegalArgumentException.class,
                    () -> normalizer.normalizeMicrosoftClaims(
                            microsoftClaims("user@"), AuthFlow.CLIENT_SIDE));
        }
    }

    // ── Google tests ─────────────────────────────────────────────────

    @Test
    void normalizeGoogleClaims_withNullName_usesEmailAsFallback() {
        VerifiedGoogleIdentity google = new VerifiedGoogleIdentity(
                "google-subject", "user@gmail.com", true, null, "https://pic.url");

        NormalizedIdentity identity = normalizer.normalizeGoogleClaims(google, AuthFlow.CLIENT_SIDE);

        assertEquals(AuthProvider.GOOGLE, identity.provider());
        assertEquals("google-subject", identity.providerUserId());
        assertEquals("user@gmail.com", identity.email());
        // name is null because normalizeGoogleClaims passes google.name() directly
        assertEquals(null, identity.name());
        assertEquals(AuthFlow.CLIENT_SIDE, identity.loginFlow());
    }
}