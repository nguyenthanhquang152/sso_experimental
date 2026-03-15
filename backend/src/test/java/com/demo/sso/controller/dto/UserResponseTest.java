package com.demo.sso.controller.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.demo.sso.model.AuthFlow;
import com.demo.sso.model.AuthProvider;
import com.demo.sso.model.User;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserResponseTest {

    @Test
    void mapsAllFieldsFromUserEntity() {
        User user = new User();
        user.setId(1L);
        user.setEmail("alice@example.com");
        user.setName("Alice");
        user.setPictureUrl("https://example.com/photo.jpg");
        user.setProvider(AuthProvider.GOOGLE);
        user.setProviderUserId("google-sub-123");
        user.setLastLoginFlow(AuthFlow.CLIENT_SIDE);
        user.setCreatedAt(Instant.parse("2024-01-15T10:30:00Z"));
        user.setLastLoginAt(Instant.parse("2024-06-01T08:00:00Z"));

        UserResponse response = UserResponse.from(user);

        assertEquals(1L, response.id());
        assertEquals("alice@example.com", response.email());
        assertEquals("Alice", response.name());
        assertEquals("https://example.com/photo.jpg", response.pictureUrl());
        assertEquals("GOOGLE", response.provider());
        assertEquals("google-sub-123", response.providerUserId());
        assertEquals("CLIENT_SIDE", response.lastLoginFlow());
        assertEquals("2024-01-15T10:30:00Z", response.createdAt());
        assertEquals("2024-06-01T08:00:00Z", response.lastLoginAt());
    }

    @Test
    void nullCreatedAtDoesNotThrowNPE() {
        User user = new User();
        user.setId(2L);
        user.setEmail("bob@example.com");
        user.setProvider(AuthProvider.MICROSOFT);
        user.setProviderUserId("ms-oid-456");
        // createdAt and lastLoginAt left null

        UserResponse response = UserResponse.from(user);

        assertEquals("", response.createdAt());
        assertEquals("", response.lastLoginAt());
    }

    @Test
    void nullLastLoginFlowReturnsNull() {
        User user = new User();
        user.setId(3L);
        user.setEmail("carol@example.com");
        user.setProvider(AuthProvider.GOOGLE);
        user.setProviderUserId("google-sub-789");
        // lastLoginFlow left null (e.g. loginMethod was removed)

        UserResponse response = UserResponse.from(user);

        assertNull(response.lastLoginFlow());
    }

    @Test
    void nullOptionalFieldsDefaultToEmptyStrings() {
        User user = new User();
        user.setId(4L);
        user.setEmail("dave@example.com");
        user.setProvider(AuthProvider.GOOGLE);
        user.setProviderUserId("google-sub-000");
        // name, pictureUrl, providerUserId all null
        user.setName(null);
        user.setPictureUrl(null);
        user.setProviderUserId(null);

        UserResponse response = UserResponse.from(user);

        assertEquals("", response.name());
        assertEquals("", response.pictureUrl());
        assertEquals("", response.providerUserId());
    }
}
