package com.demo.sso.controller.dto;

import com.demo.sso.model.AuthFlow;
import com.demo.sso.model.AuthProvider;
import com.demo.sso.model.User;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * API response for User entity. Null fields are omitted from JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        Long id,
        String email,
        String name,
        String pictureUrl,
        AuthProvider provider,
        String providerUserId,
        AuthFlow lastLoginFlow,
        String createdAt,
        String lastLoginAt
) {
    /** Maps a {@link User} entity to a {@code UserResponse}. */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPictureUrl(),
                user.getProvider(),
                user.getProviderUserId(),
                user.getLastLoginFlow(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null,
                user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null
        );
    }
}
