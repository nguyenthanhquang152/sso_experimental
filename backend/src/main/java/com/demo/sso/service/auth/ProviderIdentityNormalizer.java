package com.demo.sso.service.auth;

import com.demo.sso.exception.InvalidIdentityException;
import com.demo.sso.model.AuthFlow;
import com.demo.sso.service.model.NormalizedIdentity;
import com.demo.sso.service.token.GoogleTokenVerifier.VerifiedGoogleIdentity;
import com.demo.sso.service.token.MicrosoftIdTokenClaims;
import java.net.URI;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ProviderIdentityNormalizer {

    /**
     * @throws InvalidIdentityException if googleId or email is blank, or if email fails format validation
     */
    public NormalizedIdentity normalizeGoogleClaims(VerifiedGoogleIdentity google, AuthFlow loginFlow) {
        if (isBlank(google.subject()) || isBlank(google.email())) {
            throw new InvalidIdentityException("Google identity is missing required claims");
        }
        return NormalizedIdentity.google(
            google.subject(), normalizeEmail(google.email()), google.name(), google.pictureUrl(), loginFlow);
    }

    /**
     * Normalizes typed Microsoft identity claims into a {@link NormalizedIdentity}.
     *
     * @throws InvalidIdentityException if iss or sub is blank, if the identity is a guest/external account,
     *         if no usable email-like claim is found, or if the email fails format validation
     */
    public NormalizedIdentity normalizeMicrosoftClaims(MicrosoftIdTokenClaims claims, AuthFlow loginFlow) {
        if (isBlank(claims.iss()) || isBlank(claims.sub())) {
            throw new InvalidIdentityException("Microsoft identity is missing iss or sub");
        }

        if (!isBlank(claims.idp()) && !isBlank(claims.tid())
                && !isSameTenantAuthority(claims.idp(), claims.iss(), claims.tid())) {
            throw new InvalidIdentityException("External or guest Microsoft identities are not supported");
        }

        String email = firstNonBlank(claims.email(), claims.preferredUsername(), claims.upn());

        if (isBlank(email)) {
            throw new InvalidIdentityException("Microsoft identity is missing a usable email-like claim");
        }

        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.contains("#ext#")) {
            throw new InvalidIdentityException("Guest-style Microsoft identities are not supported");
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
        String normalizedIdp = identityProvider.trim().toLowerCase(Locale.ROOT);
        if (normalizedIdp.equals(issuer.toLowerCase(Locale.ROOT))) {
            return true;
        }
        // Parse the IDP URL and match the tenant ID as an exact path segment
        String normalizedTenantId = tenantId.toLowerCase(Locale.ROOT);
        try {
            String path = URI.create(normalizedIdp).getPath();
            if (path != null) {
                for (String segment : path.split("/")) {
                    if (!segment.isEmpty() && segment.equals(normalizedTenantId)) {
                        return true;
                    }
                }
            }
        } catch (IllegalArgumentException ignored) {
            // Not a valid URI — cannot match by path segment
        }
        return false;
    }

    private static String normalizeEmail(String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new InvalidIdentityException("Email is blank");
        }
        int atIndex = normalized.indexOf('@');
        if (atIndex < 0) {
            throw new InvalidIdentityException("Email contains no '@'");
        }
        if (atIndex != normalized.lastIndexOf('@')) {
            throw new InvalidIdentityException("Email contains multiple '@'");
        }
        if (normalized.contains(" ")) {
            throw new InvalidIdentityException("Email claim is not a usable email-like identifier");
        }
        if (atIndex == 0 || atIndex == normalized.length() - 1) {
            throw new InvalidIdentityException("Email has empty local part or domain");
        }
        return normalized;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}