package com.demo.sso.config;

import com.demo.sso.service.AuthCodeStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

@TestConfiguration
public class TestAuthCodeStoreConfig {

    @Bean
    @Primary
    public AuthCodeStore inMemoryAuthCodeStore() {
        return new InMemoryAuthCodeStore();
    }

    static class InMemoryAuthCodeStore implements AuthCodeStore {

        private static final long CODE_TTL_MS = 30_000;
        private static final SecureRandom SECURE_RANDOM = new SecureRandom();
        private final ConcurrentHashMap<String, CodeEntry> store = new ConcurrentHashMap<>();

        @Override
        public String storeJwt(String jwt) {
            evictExpired();
            byte[] bytes = new byte[32];
            SECURE_RANDOM.nextBytes(bytes);
            String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            store.put(code, new CodeEntry(jwt, System.currentTimeMillis()));
            return code;
        }

        @Override
        public String exchangeCode(String code) {
            CodeEntry entry = store.remove(code);
            if (entry == null) {
                return null;
            }
            if (System.currentTimeMillis() - entry.createdAt > CODE_TTL_MS) {
                return null;
            }
            return entry.jwt;
        }

        private void evictExpired() {
            long now = System.currentTimeMillis();
            store.entrySet().removeIf(e -> now - e.getValue().createdAt > CODE_TTL_MS);
        }

        private record CodeEntry(String jwt, long createdAt) {}
    }
}
