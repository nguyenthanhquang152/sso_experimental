package com.demo.sso.service.token;

import com.demo.sso.service.model.AuthenticatedUserIdentity;
import com.demo.sso.config.AuthRolloutProperties;
import com.demo.sso.model.AuthProvider;
import com.demo.sso.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtTokenService {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenService.class);

    private static final String ISSUER = "sso-demo-backend";
    private static final String AUDIENCE = "sso-demo-api";

    private final SecretKey key;
    private final long expirationMs;
    private final AuthRolloutProperties rolloutProperties;

    @Autowired
    public JwtTokenService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs,
            AuthRolloutProperties rolloutProperties) {
        validateSecret(secret);
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.rolloutProperties = rolloutProperties;
    }

    /** Test-only constructor; defaults to {@link AuthRolloutProperties.IdentityContractMode#LEGACY_ONLY}. */
    public JwtTokenService(String secret, long expirationMs) {
        this(secret, expirationMs, new AuthRolloutProperties());
    }

    private static void validateSecret(String secret) {
        if (secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 characters. Set the JWT_SECRET environment variable.");
        }
        String normalized = secret.trim().toLowerCase();
        if ("default".equals(normalized) || "change-me".equals(normalized)) {
            throw new IllegalStateException(
                    "JWT secret must not be a default/placeholder value. Set the JWT_SECRET environment variable.");
        }
    }

    /**
     * @deprecated Legacy token format — use {@link #generateToken(User)} instead.
     *             Retained only for backward-compatible JWT minting in LEGACY mode.
     */
    @Deprecated
    String generateLegacyToken(String email, String googleId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("googleId", googleId)
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String generateToken(User user) {
        if (rolloutProperties.getJwtMintMode() == AuthRolloutProperties.JwtMintMode.V2) {
            return generateV2Token(user);
        }
        return generateLegacyToken(user.getEmail(), user.getGoogleId());
    }

    private String generateV2Token(User user) {
        if (user.getId() == null || user.getEmail() == null || user.getProvider() == null || user.getProviderUserId() == null) {
            throw new IllegalArgumentException("V2 JWT minting requires id, email, provider, and providerUserId");
        }

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("ver", 2)
                .claim("provider", user.getProvider().name())
                .claim("providerUserId", user.getProviderUserId())
                .claim("email", user.getEmail())
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .requireAudience(AUDIENCE)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getEmailFromToken(String token) {
        return parseAuthenticatedUser(token).email();
    }

    public AuthenticatedUserIdentity parseAuthenticatedUser(String token) {
        Claims claims = parseToken(token);
        Integer version = claims.get("ver", Integer.class);

        if (version == null) {
            if (!rolloutProperties.getIdentityContractMode().acceptsLegacy()) {
                throw new IllegalArgumentException("Legacy JWTs are not accepted in "
                    + rolloutProperties.getIdentityContractMode());
            }
            logger.info("Legacy JWT identity fallback: subject={}", claims.getSubject());
            return AuthenticatedUserIdentity.legacy(
                claims.getSubject(),
                claims.get("googleId", String.class));
        }

        if (version != 2) {
            throw new IllegalArgumentException("Unsupported JWT contract version: " + version);
        }

        if (!rolloutProperties.getIdentityContractMode().acceptsV2()) {
            throw new IllegalArgumentException("V2 JWTs are not accepted in "
                + rolloutProperties.getIdentityContractMode());
        }

        String subject = claims.getSubject();
        String email = claims.get("email", String.class);
        String provider = claims.get("provider", String.class);
        String providerUserId = claims.get("providerUserId", String.class);

        if (subject == null || email == null || provider == null || providerUserId == null) {
            throw new IllegalArgumentException("V2 JWT is missing required identity claims");
        }

        try {
            return AuthenticatedUserIdentity.v2(
                Long.valueOf(subject),
                email,
                AuthProvider.valueOf(provider),
                providerUserId);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid V2 JWT identity claims", e);
        }
    }

    /**
     * Validates the token and extracts the authenticated user identity in a single parse.
     *
     * @return the identity if the token is valid, or empty if validation fails
     */
    public Optional<AuthenticatedUserIdentity> validateAndExtract(String token) {
        try {
            return Optional.of(parseAuthenticatedUser(token));
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("JWT validation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * @deprecated Use {@link #validateAndExtract(String)} instead to avoid double-parsing.
     */
    @Deprecated
    public boolean isTokenValid(String token) {
        return validateAndExtract(token).isPresent();
    }
}
