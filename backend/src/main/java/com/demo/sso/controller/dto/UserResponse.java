package com.demo.sso.controller.dto;

import com.demo.sso.model.User;

/**
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
        String name = user.getName() != null ? user.getName() : "";
        String pictureUrl = user.getPictureUrl() != null ? user.getPictureUrl() : "";
        String lastLoginFlow = user.getLastLoginFlow() != null ? user.getLastLoginFlow().name() : "";
        String createdAt = user.getCreatedAt() != null ? user.getCreatedAt().toString() : "";
        String lastLoginAt = user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : "";

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                name,
                pictureUrl,
                user.getProvider().name(),
                user.getProviderUserId(),
                lastLoginFlow,
                createdAt,
                lastLoginAt
        );
    }
}
