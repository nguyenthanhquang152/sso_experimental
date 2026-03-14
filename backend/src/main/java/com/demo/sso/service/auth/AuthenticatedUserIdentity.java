package com.demo.sso.service.auth;

import com.demo.sso.model.AuthProvider;
import java.security.Principal;

/**
 * Represents the identity claims extracted from an authenticated JWT.
 *
 * <p>Two contract versions exist:
 * <ul>
 *   <li><b>Legacy (v1)</b>: subject=email, contains a {@code googleId} claim.
 *       Created via {@link #legacy(String, String)}. Fields {@code userId},
 *       {@code provider}, {@code providerUserId}, and {@code contractVersion} are null.
 *   <li><b>V2</b>: subject=userId, contains provider-neutral identity claims.
 *       Created via {@link #v2(Long, String, AuthProvider, String)}.
 *       The {@code legacyProviderKey} field is null.
 * </ul>
 */
public record AuthenticatedUserIdentity(
        Long userId,
        String email,
        AuthProvider provider,
        String providerUserId,
        Integer contractVersion,
        String legacyProviderKey) implements Principal {

    public static AuthenticatedUserIdentity legacy(String email, String googleId) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Legacy identity requires a non-blank email");
        }
        return new AuthenticatedUserIdentity(null, email, null, null, null, googleId);
    }

    public static AuthenticatedUserIdentity v2(
            Long userId,
            String email,
            AuthProvider provider,
            String providerUserId) {
        if (userId == null || email == null || provider == null || providerUserId == null) {
            throw new IllegalArgumentException("V2 identity requires userId, email, provider, and providerUserId");
        }
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