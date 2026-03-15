package com.demo.sso.service.model;

import com.demo.sso.model.AuthProvider;
import java.security.Principal;

/**
 * Represents the identity claims extracted from an authenticated JWT.
 *
 * <p>This record uses nullable fields to model two mutually exclusive identity
 * contract versions. Exactly one of the following invariants holds:
 * <ul>
 *   <li><b>Legacy (v1)</b>: {@code contractVersion == null}, subject=email,
 *       contains a {@code legacyProviderKey} claim (typically the Google ID).
 *       Fields {@code userId}, {@code provider}, {@code providerUserId} are null.
 *       Created exclusively via {@link #legacy(String, String)}.
 *   <li><b>V2</b>: {@code contractVersion == 2}, subject=userId, contains
 *       provider-neutral identity claims. The {@code legacyProviderKey} field is null.
 *       Created exclusively via {@link #v2(Long, String, AuthProvider, String)}.
 * </ul>
 *
 * <p>Use {@link #isLegacy()} to determine which variant an instance represents.
 * Do not construct directly — always use the factory methods above.
 *
 * <h3>Why nullable fields instead of a sealed interface?</h3>
 * <p>This is a migration artifact. The nullable-variant approach was chosen for
 * backward compatibility during the legacy→V2 rollout. Once
 * {@code IdentityContractMode.V2_ONLY} is deployed and the legacy variant is
 * removed, this record should be refactored to a sealed interface with
 * {@code LegacyIdentity} and {@code V2Identity} subtypes to make the contract
 * version a type-level guarantee.
 */
// Future: refactor to sealed interface once V2_ONLY is deployed.
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