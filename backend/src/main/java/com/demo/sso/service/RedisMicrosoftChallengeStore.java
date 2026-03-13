package com.demo.sso.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
public class RedisMicrosoftChallengeStore implements MicrosoftChallengeStore {

    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);
    private static final String CHALLENGE_PREFIX = "mschallenge:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;

    public RedisMicrosoftChallengeStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public MicrosoftChallenge issueChallenge(String sessionId) {
        String challengeId = randomValue();
        String nonce = randomValue();
        redisTemplate.opsForValue().set(challengeKey(sessionId, challengeId), nonce, CHALLENGE_TTL);
        return new MicrosoftChallenge(challengeId, nonce);
    }

    @Override
    public String consumeNonce(String sessionId, String challengeId) {
        return redisTemplate.opsForValue().getAndDelete(challengeKey(sessionId, challengeId));
    }

    private static String challengeKey(String sessionId, String challengeId) {
        return CHALLENGE_PREFIX + sessionId + ":" + challengeId;
    }

    private static String randomValue() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}