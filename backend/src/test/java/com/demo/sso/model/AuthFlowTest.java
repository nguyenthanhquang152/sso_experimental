package com.demo.sso.model;

import com.demo.sso.model.AuthFlow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for AuthFlow enum.
 * Tests the enum values and the fromValue static factory method.
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
    void testFromValueWithServerSideUpperCase() {
        // Arrange & Act
        AuthFlow result = AuthFlow.fromValue("SERVER_SIDE");

        // Assert
        assertEquals(AuthFlow.SERVER_SIDE, result, "SERVER_SIDE should map to SERVER_SIDE enum");
    }

    @Test
    void testFromValueWithClientSideUpperCase() {
        // Arrange & Act
        AuthFlow result = AuthFlow.fromValue("CLIENT_SIDE");

        // Assert
        assertEquals(AuthFlow.CLIENT_SIDE, result, "CLIENT_SIDE should map to CLIENT_SIDE enum");
    }

    @Test
    void testFromValueWithServerSideLowerCase() {
        // Arrange & Act
        AuthFlow result = AuthFlow.fromValue("server_side");

        // Assert
        assertEquals(AuthFlow.SERVER_SIDE, result, "Lowercase server_side should map to SERVER_SIDE enum");
    }

    @Test
    void testFromValueWithClientSideLowerCase() {
        // Arrange & Act
        AuthFlow result = AuthFlow.fromValue("client_side");

        // Assert
        assertEquals(AuthFlow.CLIENT_SIDE, result, "Lowercase client_side should map to CLIENT_SIDE enum");
    }

    @Test
    void testFromValueWithMixedCase() {
        // Arrange & Act
        AuthFlow serverSide = AuthFlow.fromValue("SeRvEr_SiDe");
        AuthFlow clientSide = AuthFlow.fromValue("ClIeNt_SiDe");

        // Assert
        assertEquals(AuthFlow.SERVER_SIDE, serverSide, "Mixed case should be converted to uppercase");
        assertEquals(AuthFlow.CLIENT_SIDE, clientSide, "Mixed case should be converted to uppercase");
    }

    @Test
    void testFromValueWithLeadingAndTrailingWhitespace() {
        // Arrange & Act
        AuthFlow serverSide = AuthFlow.fromValue("  SERVER_SIDE  ");
        AuthFlow clientSide = AuthFlow.fromValue("\tCLIENT_SIDE\n");

        // Assert
        assertEquals(AuthFlow.SERVER_SIDE, serverSide, "Leading/trailing whitespace should be trimmed");
        assertEquals(AuthFlow.CLIENT_SIDE, clientSide, "Leading/trailing whitespace should be trimmed");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  ", "\t", "\n", "\r\n"})
    void testFromValueWithNullEmptyOrBlank(String input) {
        assertThrows(IllegalArgumentException.class,
            () -> AuthFlow.fromValue(input),
            "Null, empty, or blank input should throw IllegalArgumentException");
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "UNKNOWN", "google", "microsoft", "123", "server", "client"})
    void testFromValueWithInvalidValues(String input) {
        assertThrows(IllegalArgumentException.class,
            () -> AuthFlow.fromValue(input),
            "Invalid auth flow value should throw IllegalArgumentException: " + input);
    }

    @Test
    void testFromValueWithSpecialCharacters() {
        assertThrows(IllegalArgumentException.class, () -> AuthFlow.fromValue("SERVER-SIDE"));
        assertThrows(IllegalArgumentException.class, () -> AuthFlow.fromValue("CLIENT.SIDE"));
        assertThrows(IllegalArgumentException.class, () -> AuthFlow.fromValue("SERVER/SIDE"));
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
    void testFromValuePreservesEnumIdentity() {
        // Verify that the same enum instance is returned
        AuthFlow first = AuthFlow.fromValue("SERVER_SIDE");
        AuthFlow second = AuthFlow.fromValue("server_side");

        assertSame(first, second, "Same enum constant should be returned for equivalent inputs");
        assertSame(AuthFlow.SERVER_SIDE, first, "Should return the singleton enum instance");
    }
}
