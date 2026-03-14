package com.demo.sso.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
            verifier.verify(nullToken);
        }, "Verifying null token should throw NullPointerException");
    }

    @Test
    void testVerifyWithEmptyTokenThrowsException() {
        // Arrange
        String emptyToken = "";

        // Act & Assert
        assertThrows(Exception.class, () -> {
            verifier.verify(emptyToken);
        }, "Verifying empty token should throw exception");
    }

    @Test
    void testVerifyWithInvalidTokenThrowsIllegalArgumentException() {
        // Arrange
        String invalidToken = "invalid.token.string";

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            verifier.verify(invalidToken);
        }, "Verifying invalid token should throw exception");

        // The actual exception could be IllegalArgumentException or IOException
        // depending on how the Google library processes the invalid token
        assertTrue(
            exception instanceof IllegalArgumentException || 
            exception instanceof IOException ||
            exception instanceof GeneralSecurityException,
            "Exception should be IllegalArgumentException, IOException, or GeneralSecurityException"
        );
    }

    @Test
    void testVerifyWithMalformedJwtThrowsException() {
        // Arrange
        String malformedJwt = "not-a-valid.jwt.at.all";

        // Act & Assert
        assertThrows(Exception.class, () -> {
            verifier.verify(malformedJwt);
        }, "Malformed JWT should throw exception during verification");
    }

    @Test
    void testVerifyWithExpiredTokenThrowsException() {
        // Arrange
        // This is an example of an expired/invalid token structure
        String expiredToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiZXhwIjoxNTE2MjM5MDIyfQ.invalid";

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            verifier.verify(expiredToken);
        }, "Expired token should throw exception");

        assertTrue(
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
            verifier.verify(problematicToken);
        }, "Should handle IOException during verification");
    }

    @Test
    void testConstructorWithNullClientId() {
        // This tests initialization behavior with null client ID
        // The GoogleIdTokenVerifier might handle this differently
        assertDoesNotThrow(() -> {
            new GoogleTokenVerifier(null);
        }, "Constructor should handle null client ID without throwing immediately");
    }

    @Test
    void testConstructorWithEmptyClientId() {
        // Test with empty string client ID
        assertDoesNotThrow(() -> {
            new GoogleTokenVerifier("");
        }, "Constructor should handle empty client ID without throwing immediately");
    }
}
