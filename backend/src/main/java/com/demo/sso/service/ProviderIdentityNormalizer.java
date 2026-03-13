package com.demo.sso.service;

import com.demo.sso.model.AuthFlow;
import java.util.Locale;
import java.util.Map;
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

    public NormalizedIdentity normalizeMicrosoftClaims(Map<String, Object> claims, AuthFlow loginFlow) {
        String issuer = stringClaim(claims, "iss");
        String subject = stringClaim(claims, "sub");
        String tenantId = stringClaim(claims, "tid");

        if (isBlank(issuer) || isBlank(subject) || isBlank(tenantId)) {
            throw new IllegalArgumentException("Microsoft identity is missing iss, sub, or tid");
        }

        String expectedIssuer = "https://login.microsoftonline.com/" + tenantId + "/v2.0";
        if (!expectedIssuer.equals(issuer)) {
            throw new IllegalArgumentException("Microsoft issuer does not match tenant id");
        }

        String identityProvider = stringClaim(claims, "idp");
        if (!isBlank(identityProvider) && !isSameTenantAuthority(identityProvider, issuer, tenantId)) {
            throw new IllegalArgumentException("External or guest Microsoft identities are not supported");
        }

        String email = firstNonBlank(
            stringClaim(claims, "email"),
            stringClaim(claims, "preferred_username"),
            stringClaim(claims, "upn")
        );

        if (isBlank(email)) {
            throw new IllegalArgumentException("Microsoft identity is missing a usable email-like claim");
        }

        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.contains("#ext#")) {
            throw new IllegalArgumentException("Guest-style Microsoft identities are not supported");
        }

        return NormalizedIdentity.microsoft(
            issuer + "|" + subject,
            normalizedEmail,
            stringClaim(claims, "name"),
            stringClaim(claims, "picture"),
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

    private static String stringClaim(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static String normalizeEmail(String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        int atIndex = normalized.indexOf('@');
        if (normalized.isBlank()
                || normalized.chars().filter(ch -> ch == '@').count() != 1
                || normalized.contains(" ")
                || atIndex <= 0
                || atIndex == normalized.length() - 1) {
            throw new IllegalArgumentException("Email claim is not a usable email-like identifier");
        }
        return normalized;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}