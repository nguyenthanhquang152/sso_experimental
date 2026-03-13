package com.demo.sso.service;

import com.demo.sso.config.MicrosoftAuthProperties;
import com.demo.sso.model.AuthFlow;
import java.util.Collection;
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
        validateAudience(jwt);
        validateIssuer(jwt);
        validateVersion(jwt);
        validateNonce(jwt, expectedNonce);
        return identityNormalizer.normalizeMicrosoftClaims(jwt.getClaims(), authFlow);
    }

    private void validateAudience(Jwt jwt) {
        Object audienceClaim = jwt.getClaims().get("aud");
        if (audienceClaim instanceof Collection<?> audiences) {
            if (audiences.stream().map(String::valueOf).noneMatch(properties.getClientId()::equals)) {
                throw new IllegalArgumentException("Microsoft token audience does not match configured client id");
            }
            return;
        }

        if (!properties.getClientId().equals(String.valueOf(audienceClaim))) {
            throw new IllegalArgumentException("Microsoft token audience does not match configured client id");
        }
    }

    private static void validateIssuer(Jwt jwt) {
        String issuer = String.valueOf(jwt.getClaims().get("iss"));
        String tenantId = String.valueOf(jwt.getClaims().get("tid"));
        String expectedIssuer = "https://login.microsoftonline.com/" + tenantId + "/v2.0";
        if (!expectedIssuer.equals(issuer)) {
            throw new IllegalArgumentException("Microsoft token issuer does not match tenant id");
        }
    }

    private static void validateVersion(Jwt jwt) {
        if (!"2.0".equals(String.valueOf(jwt.getClaims().get("ver")))) {
            throw new IllegalArgumentException("Only Microsoft v2.0 ID tokens are supported");
        }
    }

    private static void validateNonce(Jwt jwt, String expectedNonce) {
        String nonce = String.valueOf(jwt.getClaims().get("nonce"));
        if (expectedNonce != null && !expectedNonce.equals(nonce)) {
            throw new IllegalArgumentException("Microsoft token nonce does not match issued challenge");
        }
    }

    private static JwtDecoder buildDecoder(MicrosoftAuthProperties properties) {
        if (!properties.isConfigured()) {
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