package com.demo.sso.controller.dto;

import com.demo.sso.model.User;

/**
 * Typed response record for the /user/me endpoint.
 * Centralizes null-coalescing for optional User fields.
 */
public record UserResponse(
        Long id,
        String email,
        String name,
        String pictureUrl,
        String provider,
        String providerUserId,
        String lastLoginFlow,
        String createdAt,
        String lastLoginAt
) {
    /** Maps a {@link User} entity to a {@code UserResponse}. */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName() != null ? user.getName() : "",
                user.getPictureUrl() != null ? user.getPictureUrl() : "",
                user.getProvider() != null ? user.getProvider().name() : "",
                user.getProviderUserId() != null ? user.getProviderUserId() : "",
                user.getLastLoginFlow() != null ? user.getLastLoginFlow().name() : null,
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : "",
                user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : ""
        );
    }
}
