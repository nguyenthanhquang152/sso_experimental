package com.demo.sso.service.model;

import com.demo.sso.exception.InvalidTokenException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Type-safe extraction of Microsoft ID token claims from raw JWT maps.
 *
 * <p>Converts {@code Map<String, Object>} claims (as returned by Spring
 * Security's {@code Jwt.getClaims()} or {@code OAuth2User.getAttributes()})
 * into strongly-typed fields in a single place, eliminating scattered
 * {@code String.valueOf(claims.get(...))} coercions throughout the codebase.
 */
public record MicrosoftIdTokenClaims(
    String iss,
    String sub,
    String tid,
    String email,
    String preferredUsername,
    String upn,
    String name,
    String picture,
    String idp,
    String ver,
    String nonce,
    List<String> audiences
) {

    /**
     * Extracts and converts all relevant Microsoft ID token claims from a raw
     * attribute map in one pass.
     *
     * @throws InvalidTokenException if the {@code sub} claim is missing or blank
     */
    public static MicrosoftIdTokenClaims fromMap(Map<String, Object> claims) {
        MicrosoftIdTokenClaims result = new MicrosoftIdTokenClaims(
            stringClaim(claims, "iss"),
            stringClaim(claims, "sub"),
            stringClaim(claims, "tid"),
            stringClaim(claims, "email"),
            stringClaim(claims, "preferred_username"),
            stringClaim(claims, "upn"),
            stringClaim(claims, "name"),
            stringClaim(claims, "picture"),
            stringClaim(claims, "idp"),
            stringClaim(claims, "ver"),
            stringClaim(claims, "nonce"),
            extractAudiences(claims.get("aud"))
        );
        if (result.sub() == null || result.sub().isBlank()) {
            throw new InvalidTokenException("Microsoft ID token must contain a 'sub' claim");
        }
        return result;
    }

    public boolean hasAudience(String clientId) {
        return audiences.contains(clientId);
    }

    private static List<String> extractAudiences(Object audienceClaim) {
        if (audienceClaim instanceof Collection<?> col) {
            return col.stream().map(String::valueOf).toList();
        }
        return audienceClaim == null ? List.of() : List.of(String.valueOf(audienceClaim));
    }

    private static String stringClaim(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return s;
        }
        return String.valueOf(value);
    }
}
