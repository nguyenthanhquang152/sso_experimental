package com.demo.sso.service.model;

import com.demo.sso.exception.InvalidTokenException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicrosoftIdTokenClaimsTest {

    private Map<String, Object> validClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", "https://login.microsoftonline.com/tenant/v2.0");
        claims.put("sub", "user-sub-123");
        claims.put("tid", "tenant-id");
        claims.put("email", "user@example.com");
        claims.put("preferred_username", "user@example.com");
        claims.put("upn", "user@example.com");
        claims.put("name", "Test User");
        claims.put("picture", "https://example.com/photo.jpg");
        claims.put("idp", "https://login.microsoftonline.com");
        claims.put("ver", "2.0");
        claims.put("nonce", "abc123");
        claims.put("aud", "client-id-456");
        return claims;
    }

    @Nested
    class FromMap {

        @Test
        void validMap_createsClaimsWithAllFields() {
            MicrosoftIdTokenClaims claims = MicrosoftIdTokenClaims.fromMap(validClaims());

            assertEquals("https://login.microsoftonline.com/tenant/v2.0", claims.iss());
            assertEquals("user-sub-123", claims.sub());
            assertEquals("tenant-id", claims.tid());
            assertEquals("user@example.com", claims.email());
            assertEquals("user@example.com", claims.preferredUsername());
            assertEquals("user@example.com", claims.upn());
            assertEquals("Test User", claims.name());
            assertEquals("https://example.com/photo.jpg", claims.picture());
            assertEquals("https://login.microsoftonline.com", claims.idp());
            assertEquals("2.0", claims.ver());
            assertEquals("abc123", claims.nonce());
            assertEquals(List.of("client-id-456"), claims.audiences());
        }

        @Test
        void missingSub_throwsInvalidTokenException() {
            Map<String, Object> claims = validClaims();
            claims.remove("sub");

            InvalidTokenException ex = assertThrows(InvalidTokenException.class,
                () -> MicrosoftIdTokenClaims.fromMap(claims));
            assertTrue(ex.getMessage().contains("sub"));
        }

        @Test
        void blankSub_throwsInvalidTokenException() {
            Map<String, Object> claims = validClaims();
            claims.put("sub", "   ");

            InvalidTokenException ex = assertThrows(InvalidTokenException.class,
                () -> MicrosoftIdTokenClaims.fromMap(claims));
            assertTrue(ex.getMessage().contains("sub"));
        }

        @Test
        void emptySub_throwsInvalidTokenException() {
            Map<String, Object> claims = validClaims();
            claims.put("sub", "");

            assertThrows(InvalidTokenException.class,
                () -> MicrosoftIdTokenClaims.fromMap(claims));
        }

        @Test
        void nullOptionalClaims_createsWithNulls() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", "user-sub-123");

            MicrosoftIdTokenClaims result = MicrosoftIdTokenClaims.fromMap(claims);

            assertNotNull(result);
            assertEquals("user-sub-123", result.sub());
            assertNull(result.iss());
            assertNull(result.tid());
            assertNull(result.email());
            assertNull(result.preferredUsername());
            assertNull(result.upn());
            assertNull(result.name());
            assertNull(result.picture());
            assertNull(result.idp());
            assertNull(result.ver());
            assertNull(result.nonce());
            assertEquals(List.of(), result.audiences());
        }

        @Test
        void audAsCollection_extractsAllAudiences() {
            Map<String, Object> claims = validClaims();
            claims.put("aud", List.of("aud-1", "aud-2", "aud-3"));

            MicrosoftIdTokenClaims result = MicrosoftIdTokenClaims.fromMap(claims);

            assertEquals(List.of("aud-1", "aud-2", "aud-3"), result.audiences());
        }

        @Test
        void audAsSingleString_extractsSingleAudience() {
            Map<String, Object> claims = validClaims();
            claims.put("aud", "single-audience");

            MicrosoftIdTokenClaims result = MicrosoftIdTokenClaims.fromMap(claims);

            assertEquals(List.of("single-audience"), result.audiences());
        }

        @Test
        void audAsNull_returnsEmptyList() {
            Map<String, Object> claims = validClaims();
            claims.put("aud", null);

            MicrosoftIdTokenClaims result = MicrosoftIdTokenClaims.fromMap(claims);

            assertEquals(List.of(), result.audiences());
        }

        @Test
        void nonStringClaimValue_convertsViaToString() {
            Map<String, Object> claims = validClaims();
            claims.put("ver", 2);

            MicrosoftIdTokenClaims result = MicrosoftIdTokenClaims.fromMap(claims);

            assertEquals("2", result.ver());
        }
    }

    @Nested
    class HasAudience {

        @Test
        void returnsTrue_whenAudienceMatches() {
            MicrosoftIdTokenClaims claims = MicrosoftIdTokenClaims.fromMap(validClaims());

            assertTrue(claims.hasAudience("client-id-456"));
        }

        @Test
        void returnsFalse_whenAudienceDoesNotMatch() {
            MicrosoftIdTokenClaims claims = MicrosoftIdTokenClaims.fromMap(validClaims());

            assertFalse(claims.hasAudience("wrong-client-id"));
        }

        @Test
        void returnsFalse_whenAudiencesIsEmpty() {
            Map<String, Object> claims = validClaims();
            claims.put("aud", null);

            MicrosoftIdTokenClaims result = MicrosoftIdTokenClaims.fromMap(claims);

            assertFalse(result.hasAudience("client-id-456"));
        }

        @Test
        void multipleAudiences_matchesAnyOne() {
            Map<String, Object> claims = validClaims();
            claims.put("aud", List.of("aud-1", "aud-2"));

            MicrosoftIdTokenClaims result = MicrosoftIdTokenClaims.fromMap(claims);

            assertTrue(result.hasAudience("aud-2"));
            assertFalse(result.hasAudience("aud-3"));
        }
    }
}
