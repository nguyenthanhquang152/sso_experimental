package com.demo.sso.config;

import com.demo.sso.exception.InvalidAuthCodeException;
import com.demo.sso.service.challenge.AuthCodeStore;
import com.demo.sso.service.challenge.MicrosoftChallengeStore;
import com.demo.sso.service.challenge.SecureCodeGenerator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@TestConfiguration
public class TestAuthCodeStoreConfig {

    @Bean
    @Primary
    public AuthCodeStore inMemoryAuthCodeStore() {
        return new InMemoryAuthCodeStore();
    }

    @Bean
    @Primary
    public MicrosoftChallengeStore inMemoryMicrosoftChallengeStore() {
        return new InMemoryMicrosoftChallengeStore();
    }

    static class InMemoryAuthCodeStore implements AuthCodeStore {

        private static final long CODE_TTL_MS = 30_000;
        private final ConcurrentHashMap<String, CodeEntry> store = new ConcurrentHashMap<>();

        @Override
        public String createAuthCode(String jwt) {
            evictExpired();
            String code = SecureCodeGenerator.generate();
            store.put(code, new CodeEntry(jwt, System.currentTimeMillis()));
            return code;
        }

        @Override
        public String exchangeCode(String code) {
            CodeEntry entry = store.remove(code);
            if (entry == null || System.currentTimeMillis() - entry.createdAt > CODE_TTL_MS) {
                throw new InvalidAuthCodeException("Invalid or expired authorization code");
            }
            return entry.jwt;
        }

        private void evictExpired() {
            long now = System.currentTimeMillis();
            store.entrySet().removeIf(e -> now - e.getValue().createdAt > CODE_TTL_MS);
        }

        private record CodeEntry(String jwt, long createdAt) {}
    }

    static class InMemoryMicrosoftChallengeStore implements MicrosoftChallengeStore {

        private static final long CHALLENGE_TTL_MS = 5 * 60_000;
        private final ConcurrentHashMap<String, ChallengeEntry> store = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, String> activeChallenges = new ConcurrentHashMap<>();

        @Override
        public MicrosoftChallenge issueChallenge(String sessionId) {
            evictExpired();
            String previousChallengeId = activeChallenges.remove(sessionId);
            if (previousChallengeId != null) {
                store.remove(sessionId + ":" + previousChallengeId);
            }

            String challengeId = SecureCodeGenerator.generate();
            String nonce = SecureCodeGenerator.generate();
            store.put(sessionId + ":" + challengeId, new ChallengeEntry(nonce, System.currentTimeMillis()));
            activeChallenges.put(sessionId, challengeId);
            return new MicrosoftChallenge(challengeId, nonce);
        }

        @Override
        public Optional<String> consumeNonce(String sessionId, String challengeId) {
            String activeChallengeId = activeChallenges.get(sessionId);
            if (!challengeId.equals(activeChallengeId)) {
                return Optional.empty();
            }

            activeChallenges.remove(sessionId);
            ChallengeEntry entry = store.remove(sessionId + ":" + challengeId);
            if (entry == null) {
                return Optional.empty();
            }
            if (System.currentTimeMillis() - entry.createdAt > CHALLENGE_TTL_MS) {
                return Optional.empty();
            }
            return Optional.of(entry.nonce);
        }

        private void evictExpired() {
            long now = System.currentTimeMillis();
            store.entrySet().removeIf(e -> {
                boolean expired = now - e.getValue().createdAt > CHALLENGE_TTL_MS;
                if (expired) {
                    int separator = e.getKey().indexOf(':');
                    if (separator > 0) {
                        String sessionId = e.getKey().substring(0, separator);
                        activeChallenges.remove(sessionId, e.getKey().substring(separator + 1));
                    }
                }
                return expired;
            });
        }

        private record ChallengeEntry(String nonce, long createdAt) {}
    }
}
