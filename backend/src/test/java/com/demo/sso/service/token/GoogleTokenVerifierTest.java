package com.demo.sso.service.token;

import com.demo.sso.service.model.VerifiedGoogleIdentity;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GoogleTokenVerifier service.
 * Tests the verification of Google ID tokens with different scenarios.
 */
class GoogleTokenVerifierTest {

    private GoogleTokenVerifier verifier;

    @BeforeEach
    void setUp() {
        // Use a test client ID for verification setup
        verifier = new GoogleTokenVerifier("test-client-id.apps.googleusercontent.com");
    }

    @Test
    void testConstructorInitializesVerifier() {
        // Arrange & Act
        GoogleTokenVerifier newVerifier = new GoogleTokenVerifier("another-client-id");

        // Assert
        assertNotNull(newVerifier, "GoogleTokenVerifier should be initialized");
    }

    @Test
    void testVerifyWithNullTokenThrowsException() {
        // Arrange
        String nullToken = null;

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            verifier.verifyIdToken(nullToken);
        }, "Verifying null token should throw NullPointerException");
    }

    @Test
    void testVerifyWithEmptyTokenThrowsException() {
        // Arrange
        String emptyToken = "";

        // Act & Assert
        assertThrows(Exception.class, () -> {
            verifier.verifyIdToken(emptyToken);
        }, "Verifying empty token should throw exception");
    }

    @Test
    void testVerifyWithInvalidTokenThrowsIllegalArgumentException() {
        // Arrange
        String invalidToken = "invalid.token.string";

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            verifier.verifyIdToken(invalidToken);
        }, "Verifying invalid token should throw exception");

        // The actual exception could be InvalidTokenException, IllegalArgumentException or IOException
        // depending on how the Google library processes the invalid token
        assertTrue(
            exception instanceof com.demo.sso.exception.InvalidTokenException ||
            exception instanceof IllegalArgumentException || 
            exception instanceof IOException ||
            exception instanceof GeneralSecurityException,
            "Exception should be InvalidTokenException, IllegalArgumentException, IOException, or GeneralSecurityException"
        );
    }

    @Test
    void testVerifyWithMalformedJwtThrowsException() {
        // Arrange
        String malformedJwt = "not-a-valid.jwt.at.all";

        // Act & Assert
        assertThrows(Exception.class, () -> {
            verifier.verifyIdToken(malformedJwt);
        }, "Malformed JWT should throw exception during verification");
    }

    @Test
    void testVerifyWithExpiredTokenThrowsException() {
        // Arrange
        // This is an example of an expired/invalid token structure
        String expiredToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiZXhwIjoxNTE2MjM5MDIyfQ.invalid";

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            verifier.verifyIdToken(expiredToken);
        }, "Expired token should throw exception");

        assertTrue(
            exception instanceof com.demo.sso.exception.InvalidTokenException ||
            exception instanceof IllegalArgumentException ||
            exception instanceof IOException ||
            exception instanceof GeneralSecurityException,
            "Should throw verification-related exception"
        );
    }

    @Test
    void testVerifyHandlesIOException() {
        // Arrange
        // Token that might cause IOException during network verification
        String problematicToken = "token.causing.io.issue";

        // Act & Assert
        assertThrows(Exception.class, () -> {
            verifier.verifyIdToken(problematicToken);
        }, "Should handle IOException during verification");
    }

    @Test
    void testConstructorWithNullClientId() {
        // This tests initialization behavior with null client ID
        // The GoogleIdTokenVerifier might handle this differently
        assertDoesNotThrow(() -> {
            new GoogleTokenVerifier((String) null);
        }, "Constructor should handle null client ID without throwing immediately");
    }

    @Test
    void testConstructorWithEmptyClientId() {
        // Test with empty string client ID
        assertDoesNotThrow(() -> {
            new GoogleTokenVerifier("");
        }, "Constructor should handle empty client ID without throwing immediately");
    }

    @Test
    void testVerifyValidToken_returnsVerifiedIdentity() throws Exception {
        // Arrange: mock the internal GoogleIdTokenVerifier to return a valid token
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("123456789");
        payload.setEmail("user@gmail.com");
        payload.setEmailVerified(true);
        payload.set("name", "Test User");
        payload.set("picture", "https://lh3.googleusercontent.com/photo.jpg");

        GoogleIdToken mockIdToken = mock(GoogleIdToken.class);
        when(mockIdToken.getPayload()).thenReturn(payload);

        GoogleIdTokenVerifier mockInternalVerifier = mock(GoogleIdTokenVerifier.class);
        when(mockInternalVerifier.verify("valid-token-string")).thenReturn(mockIdToken);

        GoogleTokenVerifier verifierWithMock = new GoogleTokenVerifier(mockInternalVerifier);

        // Act
        VerifiedGoogleIdentity identity = verifierWithMock.verifyIdToken("valid-token-string");

        // Assert
        assertNotNull(identity, "Identity should not be null for a valid token");
        assertEquals("123456789", identity.subject());
        assertEquals("user@gmail.com", identity.email());
        assertTrue(identity.emailVerified());
        assertEquals("Test User", identity.name());
        assertEquals("https://lh3.googleusercontent.com/photo.jpg", identity.pictureUrl());
    }

    @Test
    void testVerifyValidToken_withNullOptionalFields() throws Exception {
        // Arrange: verify that null optional fields (name, picture) are handled
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("987654321");
        payload.setEmail("minimal@gmail.com");
        payload.setEmailVerified(false);
        // name and picture are NOT set (null)

        GoogleIdToken mockIdToken = mock(GoogleIdToken.class);
        when(mockIdToken.getPayload()).thenReturn(payload);

        GoogleIdTokenVerifier mockInternalVerifier = mock(GoogleIdTokenVerifier.class);
        when(mockInternalVerifier.verify("minimal-token")).thenReturn(mockIdToken);

        GoogleTokenVerifier verifierWithMock = new GoogleTokenVerifier(mockInternalVerifier);

        // Act
        VerifiedGoogleIdentity identity = verifierWithMock.verifyIdToken("minimal-token");

        // Assert
        assertEquals("987654321", identity.subject());
        assertEquals("minimal@gmail.com", identity.email());
        assertFalse(identity.emailVerified());
        assertNull(identity.name());
        assertNull(identity.pictureUrl());
    }

    @Test
    void testVerifyReturnsNullToken_throwsInvalidTokenException() throws Exception {
        // Arrange: when Google verifier returns null, our wrapper should throw
        GoogleIdTokenVerifier mockInternalVerifier = mock(GoogleIdTokenVerifier.class);
        when(mockInternalVerifier.verify("unverifiable-token")).thenReturn(null);

        GoogleTokenVerifier verifierWithMock = new GoogleTokenVerifier(mockInternalVerifier);

        // Act & Assert
        com.demo.sso.exception.InvalidTokenException ex = assertThrows(
                com.demo.sso.exception.InvalidTokenException.class,
                () -> verifierWithMock.verifyIdToken("unverifiable-token"));
        assertEquals("Invalid Google ID token", ex.getMessage());
    }
}
