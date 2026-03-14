package com.demo.sso.service.token;

import com.demo.sso.service.auth.NormalizedIdentity;
import com.demo.sso.service.auth.ProviderIdentityNormalizer;
import com.demo.sso.config.MicrosoftAuthProperties;
import com.demo.sso.model.AuthFlow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

@Service
public class MicrosoftTokenVerifier {

    private final JwtDecoder jwtDecoder;
    private final MicrosoftAuthProperties properties;
    private final ProviderIdentityNormalizer identityNormalizer;

    @Autowired
    public MicrosoftTokenVerifier(MicrosoftAuthProperties properties, ProviderIdentityNormalizer identityNormalizer) {
        this(buildDecoder(properties), properties, identityNormalizer);
    }

    MicrosoftTokenVerifier(
            JwtDecoder jwtDecoder,
            MicrosoftAuthProperties properties,
            ProviderIdentityNormalizer identityNormalizer) {
        this.jwtDecoder = jwtDecoder;
        this.properties = properties;
        this.identityNormalizer = identityNormalizer;
    }

    public NormalizedIdentity verifyIdToken(String credential, String expectedNonce, AuthFlow authFlow) {
        Jwt jwt = jwtDecoder.decode(credential);
        MicrosoftIdTokenClaims claims = MicrosoftIdTokenClaims.fromMap(jwt.getClaims());
        validateAudience(claims);
        validateIssuer(claims);
        validateVersion(claims);
        validateNonce(claims, expectedNonce);
        return identityNormalizer.normalizeMicrosoftClaims(claims, authFlow);
    }

    private void validateAudience(MicrosoftIdTokenClaims claims) {
        if (!claims.hasAudience(properties.getClientId())) {
            throw new IllegalArgumentException("Microsoft token audience does not match configured client id");
        }
    }

    private static void validateIssuer(MicrosoftIdTokenClaims claims) {
        String expectedIssuer = "https://login.microsoftonline.com/" + claims.tid() + "/v2.0";
        if (!expectedIssuer.equals(claims.iss())) {
            throw new IllegalArgumentException("Microsoft token issuer does not match tenant id");
        }
    }

    private static void validateVersion(MicrosoftIdTokenClaims claims) {
        if (!"2.0".equals(claims.ver())) {
            throw new IllegalArgumentException("Only Microsoft v2.0 ID tokens are supported");
        }
    }

    private static void validateNonce(MicrosoftIdTokenClaims claims, String expectedNonce) {
        if (expectedNonce == null || expectedNonce.isBlank()) {
            throw new IllegalArgumentException("expectedNonce must not be null or blank — nonce validation cannot be skipped");
        }
        if (!expectedNonce.equals(claims.nonce())) {
            throw new IllegalArgumentException("Microsoft token nonce does not match issued challenge");
        }
    }

    private static JwtDecoder buildDecoder(MicrosoftAuthProperties properties) {
        if (!properties.isClientSideConfigured()) {
            return token -> {
                throw new IllegalStateException(
                    "Microsoft token verifier requires app.microsoft.client-id and app.microsoft.authority");
            };
        }

        return NimbusJwtDecoder.withJwkSetUri(jwksUri(properties.getAuthority())).build();
    }

    private static String jwksUri(String authority) {
        String normalizedAuthority = authority.endsWith("/")
            ? authority.substring(0, authority.length() - 1)
            : authority;
        if (normalizedAuthority.endsWith("/v2.0")) {
            return normalizedAuthority.substring(0, normalizedAuthority.length() - 5) + "/discovery/v2.0/keys";
        }
        return normalizedAuthority + "/discovery/v2.0/keys";
    }
}