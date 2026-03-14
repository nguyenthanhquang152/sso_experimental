package com.demo.sso.service.auth;

import com.demo.sso.service.token.MicrosoftIdTokenClaims;
import com.demo.sso.model.AuthFlow;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ProviderIdentityNormalizer {

    public NormalizedIdentity normalizeGoogleClaims(
            String googleId,
            String email,
            String name,
            String pictureUrl,
            AuthFlow loginFlow) {
        if (isBlank(googleId) || isBlank(email)) {
            throw new IllegalArgumentException("Google identity is missing required claims");
        }
        return NormalizedIdentity.google(googleId, normalizeEmail(email), name, pictureUrl, loginFlow);
    }

    /**
     * Normalizes typed Microsoft identity claims into a {@link NormalizedIdentity}.
     */
    public NormalizedIdentity normalizeMicrosoftClaims(MicrosoftIdTokenClaims claims, AuthFlow loginFlow) {
        if (isBlank(claims.iss()) || isBlank(claims.sub())) {
            throw new IllegalArgumentException("Microsoft identity is missing iss or sub");
        }

        if (!isBlank(claims.idp()) && !isBlank(claims.tid())
                && !isSameTenantAuthority(claims.idp(), claims.iss(), claims.tid())) {
            throw new IllegalArgumentException("External or guest Microsoft identities are not supported");
        }

        String email = firstNonBlank(claims.email(), claims.preferredUsername(), claims.upn());

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

    private static String firstNonBlank(String... candidates) {
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
        int atIndex = normalized.indexOf('@');
        boolean hasExactlyOneAt = normalized.chars().filter(ch -> ch == '@').count() == 1;
        boolean hasNoSpaces = !normalized.contains(" ");
        boolean hasNonEmptyLocalAndDomain = atIndex > 0 && atIndex < normalized.length() - 1;
        if (normalized.isBlank() || !hasExactlyOneAt || !hasNoSpaces || !hasNonEmptyLocalAndDomain) {
            throw new IllegalArgumentException("Email claim is not a usable email-like identifier");
        }
        return normalized;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}