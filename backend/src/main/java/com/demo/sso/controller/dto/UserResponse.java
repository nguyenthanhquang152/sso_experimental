package com.demo.sso.controller.dto;

import com.demo.sso.model.User;

public record UserResponse(
        long id,
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

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                orEmpty(user.getName()),
                orEmpty(user.getPictureUrl()),
                orEmpty(user.getLoginMethod()),
                user.getProvider() != null ? user.getProvider().name() : "",
                orEmpty(user.getProviderUserId()),
                user.getLastLoginFlow() != null ? user.getLastLoginFlow().name() : "",
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : "",
                user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : ""
        );
    }

    private static String orEmpty(String value) {
        return value != null ? value : "";
    }
}
