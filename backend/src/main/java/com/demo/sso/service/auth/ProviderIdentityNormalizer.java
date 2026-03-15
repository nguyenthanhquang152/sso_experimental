package com.demo.sso.service.auth;

import com.demo.sso.model.AuthFlow;
import com.demo.sso.service.model.NormalizedIdentity;
import com.demo.sso.service.token.GoogleTokenVerifier.VerifiedGoogleIdentity;
import com.demo.sso.service.token.MicrosoftIdTokenClaims;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ProviderIdentityNormalizer {

    /**
     * @throws IllegalArgumentException if googleId or email is blank, or if email fails format validation
     */
    public NormalizedIdentity normalizeGoogleClaims(VerifiedGoogleIdentity google, AuthFlow loginFlow) {
        if (isBlank(google.subject()) || isBlank(google.email())) {
            throw new IllegalArgumentException("Google identity is missing required claims");
        }
        return NormalizedIdentity.google(
            google.subject(), normalizeEmail(google.email()), google.name(), google.pictureUrl(), loginFlow);
    }

    /**
     * Normalizes typed Microsoft identity claims into a {@link NormalizedIdentity}.
     *
     * @throws IllegalArgumentException if iss or sub is blank, if the identity is a guest/external account,
     *         if no usable email-like claim is found, or if the email fails format validation
     */
    public NormalizedIdentity normalizeMicrosoftClaims(MicrosoftIdTokenClaims claims, AuthFlow loginFlow) {
        if (isBlank(claims.iss()) || isBlank(claims.sub())) {
            throw new IllegalArgumentException("Microsoft identity is missing iss or sub");
        }

        if (!isBlank(claims.idp()) && !isBlank(claims.tid())
                && !isSameTenantAuthority(claims.idp(), claims.iss(), claims.tid())) {
            throw new IllegalArgumentException("External or guest Microsoft identities are not supported");
        }

        String email = firstPresent(claims.email(), claims.preferredUsername(), claims.upn());

        if (isBlank(email)) {
            throw new IllegalArgumentException("Microsoft identity is missing a usable email-like claim");
        }

        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.contains("#ext#")) {
            throw new IllegalArgumentException("Guest-style Microsoft identities are not supported");
        }

        return NormalizedIdentity.microsoft(
            claims.iss() + "|" + claims.sub(),
            normalizedEmail,
            claims.name(),
            claims.picture(),
            loginFlow
        );
    }

    private static String firstPresent(String... candidates) {
        for (String candidate : candidates) {
            if (!isBlank(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isSameTenantAuthority(String identityProvider, String issuer, String tenantId) {
        String normalized = identityProvider.trim().toLowerCase(Locale.ROOT);
        return normalized.equals(issuer.toLowerCase(Locale.ROOT))
            || normalized.contains(tenantId.toLowerCase(Locale.ROOT));
    }

    private static String normalizeEmail(String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Email is blank");
        }
        int atIndex = normalized.indexOf('@');
        if (atIndex < 0) {
            throw new IllegalArgumentException("Email contains no '@'");
        }
        if (atIndex != normalized.lastIndexOf('@')) {
            throw new IllegalArgumentException("Email contains multiple '@'");
        }
        if (normalized.contains(" ")) {
            throw new IllegalArgumentException("Email claim is not a usable email-like identifier");
        }
        if (atIndex == 0 || atIndex == normalized.length() - 1) {
            throw new IllegalArgumentException("Email has empty local part or domain");
        }
        return normalized;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}