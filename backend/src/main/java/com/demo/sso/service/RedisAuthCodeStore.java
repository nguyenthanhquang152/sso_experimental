package com.demo.sso.service;

import com.demo.sso.config.AuthRolloutProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Service
@Profile("!test")
public class RedisAuthCodeStore implements AuthCodeStore {

    private static final Duration CODE_TTL = Duration.ofSeconds(30);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String LEGACY_KEY_PREFIX = "authcode:";
    private static final String V2_KEY_PREFIX = "authcode:v2:";

    private final StringRedisTemplate redisTemplate;
    private final AuthRolloutProperties rolloutProperties;

    public RedisAuthCodeStore(StringRedisTemplate redisTemplate, AuthRolloutProperties rolloutProperties) {
        this.redisTemplate = redisTemplate;
        this.rolloutProperties = rolloutProperties;
    }

    public RedisAuthCodeStore(StringRedisTemplate redisTemplate) {
        this(redisTemplate, new AuthRolloutProperties());
    }

    @Override
    public String storeJwt(String jwt) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        redisTemplate.opsForValue().set(currentWritePrefix() + code, jwt, CODE_TTL);
        return code;
    }

    @Override
    public String exchangeCode(String code) {
        // GETDEL: atomically get and delete — guarantees single-use
        if (rolloutProperties.getIdentityContractMode() == AuthRolloutProperties.IdentityContractMode.V2_ONLY) {
            return redisTemplate.opsForValue().getAndDelete(V2_KEY_PREFIX + code);
        }

        String legacyJwt = redisTemplate.opsForValue().getAndDelete(LEGACY_KEY_PREFIX + code);
        if (legacyJwt != null) {
            return legacyJwt;
        }

        if (rolloutProperties.getIdentityContractMode().acceptsV2()) {
            return redisTemplate.opsForValue().getAndDelete(V2_KEY_PREFIX + code);
        }

        return null;
    }

    private String currentWritePrefix() {
        return rolloutProperties.getJwtMintMode() == AuthRolloutProperties.JwtMintMode.V2
            ? V2_KEY_PREFIX
            : LEGACY_KEY_PREFIX;
    }
}
