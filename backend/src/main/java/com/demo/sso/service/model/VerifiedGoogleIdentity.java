package com.demo.sso.service.model;

import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Provider-neutral representation of a verified Google identity.
 * {@code name} and {@code pictureUrl} may be null if the user hasn't set them.
 */
public record VerifiedGoogleIdentity(
        String subject,
        String email,
        boolean emailVerified,
        String name,
        String pictureUrl) {

    /** Creates a VerifiedGoogleIdentity from server-side OAuth2 user attributes. */
    public static VerifiedGoogleIdentity fromOAuth2User(OAuth2User user) {
        return new VerifiedGoogleIdentity(
                user.getAttribute("sub"),
                user.getAttribute("email"),
                Boolean.TRUE.equals(user.getAttribute("email_verified")),
                user.getAttribute("name"),
                user.getAttribute("picture"));
    }
}
