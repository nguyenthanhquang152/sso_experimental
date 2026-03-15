package com.demo.sso.service.challenge;

import com.demo.sso.config.properties.AuthRolloutProperties;
import com.demo.sso.exception.InvalidAuthCodeException;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed auth-code store with dual key-prefix routing for the legacy→V2 migration.
 *
 * <p>Uses separate key prefixes ({@value #LEGACY_KEY_PREFIX} vs {@value #V2_KEY_PREFIX})
 * so legacy and V2 auth codes can coexist during the JWT identity migration.
 * Remove the legacy prefix once all deployments run {@code JwtMintMode.V2}
 * and the {@value #CODE_TTL} TTL has elapsed since the last legacy mint.
 *
 * @see AuthRolloutProperties.JwtMintMode
 * @see AuthRolloutProperties.IdentityContractMode
 */
@Service
@Profile("!test")
public class RedisAuthCodeStore implements AuthCodeStore {

    private static final Duration CODE_TTL = Duration.ofSeconds(30);
    private static final String LEGACY_KEY_PREFIX = "authcode:";
    private static final String V2_KEY_PREFIX = "authcode:v2:";

    private final StringRedisTemplate redisTemplate;
    private final AuthRolloutProperties rolloutProperties;

    @Autowired
    public RedisAuthCodeStore(StringRedisTemplate redisTemplate, AuthRolloutProperties rolloutProperties) {
        this.redisTemplate = redisTemplate;
        this.rolloutProperties = rolloutProperties;
    }

    RedisAuthCodeStore(StringRedisTemplate redisTemplate) {
        this(redisTemplate, new AuthRolloutProperties());
    }

    @Override
    public String createAuthCode(String jwt) {
        if (jwt == null || jwt.isBlank()) {
            throw new IllegalArgumentException("jwt must not be null or blank");
        }
        String code = SecureCodeGenerator.generate();
        redisTemplate.opsForValue().set(currentWritePrefix() + code, jwt, CODE_TTL);
        return code;
    }

    // Lookup order: V2_ONLY mode checks only v2 keys. LEGACY_ONLY checks only legacy keys.
    // COMPATIBILITY mode checks legacy first (backward compat), then v2 (forward compat).
    @Override
    public String exchangeCode(String code) {
        return switch (rolloutProperties.getIdentityContractMode()) {
            case V2_ONLY -> exchangeOrThrow(V2_KEY_PREFIX + code);
            case LEGACY_ONLY -> exchangeOrThrow(LEGACY_KEY_PREFIX + code);
            case COMPATIBILITY -> exchangeWithCompatFallback(code);
        };
    }

    private String exchangeOrThrow(String key) {
        String jwt = redisTemplate.opsForValue().getAndDelete(key);
        if (jwt == null) {
            throw new InvalidAuthCodeException("Invalid or expired authorization code");
        }
        return jwt;
    }

    private String exchangeWithCompatFallback(String code) {
        String legacyJwt = redisTemplate.opsForValue().getAndDelete(LEGACY_KEY_PREFIX + code);
        if (legacyJwt != null) {
            return legacyJwt;
        }
        return exchangeOrThrow(V2_KEY_PREFIX + code);
    }

    private String currentWritePrefix() {
        return rolloutProperties.getJwtMintMode() == AuthRolloutProperties.JwtMintMode.V2
            ? V2_KEY_PREFIX
            : LEGACY_KEY_PREFIX;
    }
}
