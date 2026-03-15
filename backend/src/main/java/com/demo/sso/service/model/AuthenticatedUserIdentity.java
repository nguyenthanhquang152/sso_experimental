package com.demo.sso.service.model;

import com.demo.sso.model.AuthProvider;
import java.security.Principal;

/**
 * Identity claims extracted from an authenticated JWT. Uses nullable fields
 * to model legacy (v1) and V2 identity contracts. Use {@link #isLegacy()}
 * to determine the variant; construct via factory methods only.
 *
 * @see #legacy(String, String)
 * @see #v2(Long, String, AuthProvider, String)
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