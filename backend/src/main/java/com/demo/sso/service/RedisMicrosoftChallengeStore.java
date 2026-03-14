package com.demo.sso.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
public class RedisMicrosoftChallengeStore implements MicrosoftChallengeStore {

    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);
    private static final String CHALLENGE_PREFIX = "mschallenge:";
    private static final String ACTIVE_CHALLENGE_PREFIX = "mschallenge-active:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;

    public RedisMicrosoftChallengeStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public MicrosoftChallenge issueChallenge(String sessionId) {
        String previousChallengeId = redisTemplate.opsForValue().get(activeChallengeKey(sessionId));
        if (previousChallengeId != null && !previousChallengeId.isBlank()) {
            redisTemplate.delete(challengeKey(sessionId, previousChallengeId));
        }

        String challengeId = randomValue();
        String nonce = randomValue();
        redisTemplate.opsForValue().set(challengeKey(sessionId, challengeId), nonce, CHALLENGE_TTL);
        redisTemplate.opsForValue().set(activeChallengeKey(sessionId), challengeId, CHALLENGE_TTL);
        return new MicrosoftChallenge(challengeId, nonce);
    }

    @Override
    public Optional<String> consumeNonce(String sessionId, String challengeId) {
        String activeChallengeId = redisTemplate.opsForValue().get(activeChallengeKey(sessionId));
        if (!challengeId.equals(activeChallengeId)) {
            return Optional.empty();
        }

        String nonce = redisTemplate.opsForValue().getAndDelete(challengeKey(sessionId, challengeId));
        if (nonce == null) {
            return Optional.empty();
        }

        redisTemplate.delete(activeChallengeKey(sessionId));
        return Optional.of(nonce);
    }

    private static String challengeKey(String sessionId, String challengeId) {
        return CHALLENGE_PREFIX + sessionId + ":" + challengeId;
    }

    private static String activeChallengeKey(String sessionId) {
        return ACTIVE_CHALLENGE_PREFIX + sessionId;
    }

    private static String randomValue() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}