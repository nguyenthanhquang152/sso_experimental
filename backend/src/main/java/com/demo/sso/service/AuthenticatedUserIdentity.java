package com.demo.sso.service;

import com.demo.sso.model.AuthProvider;
import java.security.Principal;

public record AuthenticatedUserIdentity(
        Long userId,
        String email,
        AuthProvider provider,
        String providerUserId,
        Integer contractVersion,
        String legacyGoogleId) implements Principal {

    public static AuthenticatedUserIdentity legacy(String email, String googleId) {
        return new AuthenticatedUserIdentity(null, email, null, null, null, googleId);
    }

    public static AuthenticatedUserIdentity v2(
            Long userId,
            String email,
            AuthProvider provider,
            String providerUserId) {
        return new AuthenticatedUserIdentity(userId, email, provider, providerUserId, 2, null);
    }

    public boolean isLegacy() {
        return contractVersion == null;
    }

    @Override
    public String getName() {
        return isLegacy() ? email : String.valueOf(userId);
    }
}