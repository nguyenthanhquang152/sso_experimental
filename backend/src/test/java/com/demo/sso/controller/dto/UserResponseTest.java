package com.demo.sso.controller.dto;

import com.demo.sso.model.AuthFlow;
import com.demo.sso.model.AuthProvider;
import com.demo.sso.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class UserResponseTest {

    @Test
    void mapsAllFieldsFromFullyPopulatedUser() {
        User user = new User();
        user.setId(42L);
        user.setEmail("test@example.com");
        user.setName("Test User");
        user.setPictureUrl("https://example.com/pic.jpg");
        user.setLoginMethod("SERVER_SIDE");
        user.setProvider(AuthProvider.GOOGLE);
        user.setProviderUserId("google-123");
        user.setLastLoginFlow(AuthFlow.SERVER_SIDE);
        Instant now = Instant.parse("2026-03-15T10:00:00Z");
        user.setCreatedAt(now);
        user.setLastLoginAt(now);

        UserResponse response = UserResponse.from(user);

        assertEquals(42L, response.id());
        assertEquals("test@example.com", response.email());
        assertEquals("Test User", response.name());
        assertEquals("https://example.com/pic.jpg", response.pictureUrl());
        assertEquals("SERVER_SIDE", response.loginMethod());
        assertEquals("GOOGLE", response.provider());
        assertEquals("google-123", response.providerUserId());
        assertEquals("SERVER_SIDE", response.lastLoginFlow());
        assertEquals("2026-03-15T10:00:00Z", response.createdAt());
        assertEquals("2026-03-15T10:00:00Z", response.lastLoginAt());
    }

    @Test
    void mapsNullOptionalFieldsToEmptyStrings() {
        User user = new User();
        user.setId(1L);
        user.setEmail("min@example.com");
        user.setProvider(AuthProvider.MICROSOFT);
        user.setProviderUserId("ms-456");
        // name, pictureUrl, loginMethod, lastLoginFlow, createdAt, lastLoginAt are null

        UserResponse response = UserResponse.from(user);

        assertEquals(1L, response.id());
        assertEquals("min@example.com", response.email());
        assertEquals("", response.name());
        assertEquals("", response.pictureUrl());
        assertEquals("", response.loginMethod());
        assertEquals("MICROSOFT", response.provider());
        assertEquals("ms-456", response.providerUserId());
        assertEquals("", response.lastLoginFlow());
        assertEquals("", response.createdAt());
        assertEquals("", response.lastLoginAt());
    }

    @Test
    void handlesNullProviderGracefully() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setProviderUserId("id-1");
        // provider is null

        UserResponse response = UserResponse.from(user);

        assertEquals("", response.provider());
    }
}
