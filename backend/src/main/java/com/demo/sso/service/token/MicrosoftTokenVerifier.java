package com.demo.sso.service.token;

import com.demo.sso.config.properties.MicrosoftAuthProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

@Service
public class MicrosoftTokenVerifier {

    private static final String JWKS_DISCOVERY_PATH = "/discovery/v2.0/keys";
    private static final String V2_SUFFIX = "/v2.0";

    private final JwtDecoder jwtDecoder;
    private final MicrosoftAuthProperties properties;

    @Autowired
    public MicrosoftTokenVerifier(MicrosoftAuthProperties properties) {
        this(buildDecoder(properties), properties);
    }

    MicrosoftTokenVerifier(JwtDecoder jwtDecoder, MicrosoftAuthProperties properties) {
        this.jwtDecoder = jwtDecoder;
        this.properties = properties;
    }

    public MicrosoftIdTokenClaims verifyIdToken(String credential, String expectedNonce) {
        Jwt jwt = jwtDecoder.decode(credential);
        MicrosoftIdTokenClaims claims = MicrosoftIdTokenClaims.fromMap(jwt.getClaims());
        validateAudience(claims);
        validateIssuer(claims);
        validateVersion(claims);
        validateNonce(claims, expectedNonce);
        return claims;
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
        String base = authority.endsWith("/")
            ? authority.substring(0, authority.length() - 1)
            : authority;
        if (base.endsWith(V2_SUFFIX)) {
            base = base.substring(0, base.length() - V2_SUFFIX.length());
        }
        return base + JWKS_DISCOVERY_PATH;
    }
}