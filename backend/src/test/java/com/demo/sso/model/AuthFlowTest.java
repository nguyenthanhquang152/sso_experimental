package com.demo.sso.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuthFlow enum.
 * Tests the enum values and the fromLoginMethod static factory method.
 */
class AuthFlowTest {

    @Test
    void testEnumValues() {
        // Verify enum constants exist
        AuthFlow[] values = AuthFlow.values();
        assertEquals(2, values.length, "AuthFlow should have exactly 2 values");
        
        // Verify specific enum values
        assertEquals(AuthFlow.SERVER_SIDE, AuthFlow.valueOf("SERVER_SIDE"));
        assertEquals(AuthFlow.CLIENT_SIDE, AuthFlow.valueOf("CLIENT_SIDE"));
    }

    @Test
    void testFromLoginMethodWithServerSideUpperCase() {
        // Arrange & Act
        AuthFlow result = AuthFlow.fromLoginMethod("SERVER_SIDE");

        // Assert
        assertEquals(AuthFlow.SERVER_SIDE, result, "SERVER_SIDE should map to SERVER_SIDE enum");
    }

    @Test
    void testFromLoginMethodWithClientSideUpperCase() {
        // Arrange & Act
        AuthFlow result = AuthFlow.fromLoginMethod("CLIENT_SIDE");

        // Assert
        assertEquals(AuthFlow.CLIENT_SIDE, result, "CLIENT_SIDE should map to CLIENT_SIDE enum");
    }

    @Test
    void testFromLoginMethodWithServerSideLowerCase() {
        // Arrange & Act
        AuthFlow result = AuthFlow.fromLoginMethod("server_side");

        // Assert
        assertEquals(AuthFlow.SERVER_SIDE, result, "Lowercase server_side should map to SERVER_SIDE enum");
    }

    @Test
    void testFromLoginMethodWithClientSideLowerCase() {
        // Arrange & Act
        AuthFlow result = AuthFlow.fromLoginMethod("client_side");

        // Assert
        assertEquals(AuthFlow.CLIENT_SIDE, result, "Lowercase client_side should map to CLIENT_SIDE enum");
    }

    @Test
    void testFromLoginMethodWithMixedCase() {
        // Arrange & Act
        AuthFlow serverSide = AuthFlow.fromLoginMethod("SeRvEr_SiDe");
        AuthFlow clientSide = AuthFlow.fromLoginMethod("ClIeNt_SiDe");

        // Assert
        assertEquals(AuthFlow.SERVER_SIDE, serverSide, "Mixed case should be converted to uppercase");
        assertEquals(AuthFlow.CLIENT_SIDE, clientSide, "Mixed case should be converted to uppercase");
    }

    @Test
    void testFromLoginMethodWithLeadingAndTrailingWhitespace() {
        // Arrange & Act
        AuthFlow serverSide = AuthFlow.fromLoginMethod("  SERVER_SIDE  ");
        AuthFlow clientSide = AuthFlow.fromLoginMethod("\tCLIENT_SIDE\n");

        // Assert
        assertEquals(AuthFlow.SERVER_SIDE, serverSide, "Leading/trailing whitespace should be trimmed");
        assertEquals(AuthFlow.CLIENT_SIDE, clientSide, "Leading/trailing whitespace should be trimmed");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  ", "\t", "\n", "\r\n"})
    void testFromLoginMethodWithNullEmptyOrBlank(String input) {
        // Act
        AuthFlow result = AuthFlow.fromLoginMethod(input);

        // Assert
        assertNull(result, "Null, empty, or blank input should return null");
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "UNKNOWN", "google", "microsoft", "123", "server", "client"})
    void testFromLoginMethodWithInvalidValues(String input) {
        // Act
        AuthFlow result = AuthFlow.fromLoginMethod(input);

        // Assert
        assertNull(result, "Invalid auth flow value should return null: " + input);
    }

    @Test
    void testFromLoginMethodWithSpecialCharacters() {
        // Arrange & Act
        AuthFlow result1 = AuthFlow.fromLoginMethod("SERVER-SIDE");
        AuthFlow result2 = AuthFlow.fromLoginMethod("CLIENT.SIDE");
        AuthFlow result3 = AuthFlow.fromLoginMethod("SERVER/SIDE");

        // Assert - these don't match the exact enum format
        assertNull(result1, "Special characters should not match enum values");
        assertNull(result2, "Special characters should not match enum values");
        assertNull(result3, "Special characters should not match enum values");
    }

    @Test
    void testEnumToString() {
        // Verify enum toString provides expected values
        assertEquals("SERVER_SIDE", AuthFlow.SERVER_SIDE.toString());
        assertEquals("CLIENT_SIDE", AuthFlow.CLIENT_SIDE.toString());
    }

    @Test
    void testEnumName() {
        // Verify enum name() provides expected values
        assertEquals("SERVER_SIDE", AuthFlow.SERVER_SIDE.name());
        assertEquals("CLIENT_SIDE", AuthFlow.CLIENT_SIDE.name());
    }

    @Test
    void testEnumOrdinal() {
        // Verify ordinal values (based on declaration order)
        assertEquals(0, AuthFlow.SERVER_SIDE.ordinal());
        assertEquals(1, AuthFlow.CLIENT_SIDE.ordinal());
    }

    @Test
    void testFromLoginMethodPreservesEnumIdentity() {
        // Verify that the same enum instance is returned
        AuthFlow first = AuthFlow.fromLoginMethod("SERVER_SIDE");
        AuthFlow second = AuthFlow.fromLoginMethod("server_side");

        assertSame(first, second, "Same enum constant should be returned for equivalent inputs");
        assertSame(AuthFlow.SERVER_SIDE, first, "Should return the singleton enum instance");
    }
}
