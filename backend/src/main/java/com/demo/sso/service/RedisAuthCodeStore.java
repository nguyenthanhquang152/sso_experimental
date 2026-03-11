package com.demo.sso.service;

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
    private static final String KEY_PREFIX = "authcode:";

    private final StringRedisTemplate redisTemplate;

    public RedisAuthCodeStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String storeJwt(String jwt) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        redisTemplate.opsForValue().set(KEY_PREFIX + code, jwt, CODE_TTL);
        return code;
    }

    @Override
    public String exchangeCode(String code) {
        // GETDEL: atomically get and delete — guarantees single-use
        return redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + code);
    }
}
