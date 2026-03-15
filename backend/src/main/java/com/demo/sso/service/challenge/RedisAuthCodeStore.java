package com.demo.sso.service.challenge;

import com.demo.sso.config.properties.AuthRolloutProperties;
import com.demo.sso.exception.ExpiredAuthCodeException;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed auth-code store with dual key-prefix routing for the legacy→V2 migration.
 *
 * <h3>Why two key prefixes?</h3>
 * <p>During the JWT identity migration ({@link AuthRolloutProperties.JwtMintMode}),
 * legacy tokens and V2 tokens coexist. The store uses different Redis key prefixes
 * ({@value #LEGACY_KEY_PREFIX} vs {@value #V2_KEY_PREFIX}) so that the exchange
 * endpoint can look up codes written by either mint mode.
 *
 * <ul>
 *   <li><b>Write path:</b> prefix is chosen by the current {@code JwtMintMode} —
 *       see {@link #currentWritePrefix()}.
 *   <li><b>Read path:</b> prefix is chosen by the current {@code IdentityContractMode} —
 *       see {@link #exchangeCode(String)}.
 * </ul>
 *
 * <h3>When can this be removed?</h3>
 * <p>Once all deployments run with {@code JwtMintMode.V2} and no legacy auth codes
 * remain in Redis (i.e., after the {@value #CODE_TTL} TTL has elapsed since the
 * last legacy mint), the {@code LEGACY_KEY_PREFIX} branch and compatibility
 * fallback can be deleted. Track via the {@code IdentityContractMode.V2_ONLY}
 * rollout phase.
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

    public RedisAuthCodeStore(StringRedisTemplate redisTemplate) {
        this(redisTemplate, new AuthRolloutProperties());
    }

    @Override
    public String createAuthCode(String jwt) {
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
            throw new ExpiredAuthCodeException("Invalid or expired authorization code");
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
