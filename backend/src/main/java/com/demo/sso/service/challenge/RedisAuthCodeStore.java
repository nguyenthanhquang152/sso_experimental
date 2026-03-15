package com.demo.sso.service.challenge;

import com.demo.sso.config.properties.AuthRolloutProperties;
import com.demo.sso.exception.InvalidAuthCodeException;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed auth-code store using the V2 key prefix.
 *
 * <p>Legacy key prefix support has been removed. All new auth codes use the
 * {@value #V2_KEY_PREFIX} namespace. Reading still falls back to the legacy
 * prefix during the TTL window to avoid breaking in-flight codes.
 */
@Service
@Profile("!test")
public class RedisAuthCodeStore implements AuthCodeStore {

    private static final Duration CODE_TTL = Duration.ofSeconds(30);
    private static final String LEGACY_KEY_PREFIX = "authcode:";
    private static final String V2_KEY_PREFIX = "authcode:v2:";

    private final StringRedisTemplate redisTemplate;

    @Autowired
    public RedisAuthCodeStore(StringRedisTemplate redisTemplate, AuthRolloutProperties rolloutProperties) {
        this.redisTemplate = redisTemplate;
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
        redisTemplate.opsForValue().set(V2_KEY_PREFIX + code, jwt, CODE_TTL);
        return code;
    }

    @Override
    public String exchangeCode(String code) {
        String jwt = redisTemplate.opsForValue().getAndDelete(V2_KEY_PREFIX + code);
        if (jwt != null) {
            return jwt;
        }
        jwt = redisTemplate.opsForValue().getAndDelete(LEGACY_KEY_PREFIX + code);
        if (jwt != null) {
            return jwt;
        }
        throw new InvalidAuthCodeException("Invalid or expired authorization code");
    }
}
