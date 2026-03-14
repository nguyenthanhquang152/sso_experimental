package com.demo.sso.service.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.demo.sso.model.AuthFlow;
import com.demo.sso.model.AuthProvider;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProviderIdentityNormalizerTest {

    private ProviderIdentityNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new ProviderIdentityNormalizer();
    }

    @Test
    void normalizeMicrosoftClaims_usesIssuerScopedSubjectAndPrimaryEmail() {
        NormalizedIdentity identity = normalizer.normalizeMicrosoftClaims(Map.of(
            "iss", "https://login.microsoftonline.com/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/v2.0",
            "sub", "microsoft-subject",
            "tid", "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
            "email", "User@Example.com",
            "name", "Microsoft User"
        ), AuthFlow.CLIENT_SIDE);

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
        NormalizedIdentity identity = normalizer.normalizeMicrosoftClaims(Map.of(
            "iss", "https://login.microsoftonline.com/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/v2.0",
            "sub", "microsoft-subject",
            "tid", "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
            "preferred_username", "Preferred.User@example.com"
        ), AuthFlow.SERVER_SIDE);

        assertEquals("preferred.user@example.com", identity.email());
        assertEquals(AuthFlow.SERVER_SIDE, identity.loginFlow());
    }

    @Test
    void normalizeMicrosoftClaims_rejectsGuestStyleIdentifiers() {
        assertThrows(IllegalArgumentException.class, () -> normalizer.normalizeMicrosoftClaims(Map.of(
            "iss", "https://login.microsoftonline.com/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/v2.0",
            "sub", "microsoft-subject",
            "tid", "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
            "preferred_username", "guest_user#EXT#@example.onmicrosoft.com"
        ), AuthFlow.CLIENT_SIDE));
    }

    @Test
    void normalizeMicrosoftClaims_rejectsExternalIdentityProviders() {
        assertThrows(IllegalArgumentException.class, () -> normalizer.normalizeMicrosoftClaims(Map.of(
            "iss", "https://login.microsoftonline.com/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/v2.0",
            "sub", "microsoft-subject",
            "tid", "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
            "email", "user@example.com",
            "idp", "live.com"
        ), AuthFlow.CLIENT_SIDE));
    }
}