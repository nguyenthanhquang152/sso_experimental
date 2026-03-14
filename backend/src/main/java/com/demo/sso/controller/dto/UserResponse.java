package com.demo.sso.controller.dto;

import com.demo.sso.model.User;

/**
 * Typed response record for the /user/me endpoint.
 * Provides compile-time safety for field names and types,
 * and centralizes null-coalescing for optional User fields.
 */
public record UserResponse(
        Long id,
        String email,
        String name,
        String pictureUrl,
        String loginMethod,
        String provider,
        String providerUserId,
        String lastLoginFlow,
        String createdAt,
        String lastLoginAt
) {
    /** Factory method that maps a {@link User} entity to a {@code UserResponse}. */
    public static UserResponse from(User user) {
        String loginFlow = user.getLastLoginFlow() != null ? user.getLastLoginFlow().name() : "";
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName() != null ? user.getName() : "",
                user.getPictureUrl() != null ? user.getPictureUrl() : "",
                loginFlow,
                user.getProvider() != null ? user.getProvider().name() : "",
                user.getProviderUserId() != null ? user.getProviderUserId() : "",
                loginFlow,
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : "",
                user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : ""
        );
    }
}
