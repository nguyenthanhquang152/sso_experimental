package com.demo.sso.service;

import com.demo.sso.service.MicrosoftChallengeStore;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisMicrosoftChallengeStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private RedisMicrosoftChallengeStore store;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        store = new RedisMicrosoftChallengeStore(redisTemplate);
    }

    @Test
    void issueChallenge_storesNonceWithTtl() {
        MicrosoftChallengeStore.MicrosoftChallenge challenge = store.issueChallenge("session-123");

        assertNotNull(challenge.challengeId());
        assertNotNull(challenge.nonce());
        assertTrue(challenge.challengeId().length() > 10);
        assertTrue(challenge.nonce().length() > 10);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

        verify(valueOps).set(keyCaptor.capture(), valueCaptor.capture(), ttlCaptor.capture());
        assertTrue(keyCaptor.getValue().startsWith("mschallenge:session-123:"));
        assertEquals(challenge.nonce(), valueCaptor.getValue());
        assertEquals(Duration.ofMinutes(5), ttlCaptor.getValue());
    }

    @Test
    void consumeNonce_readsAndDeletesChallenge() {
        when(valueOps.getAndDelete("mschallenge:session-123:challenge-123")).thenReturn("nonce-123");

        String nonce = store.consumeNonce("session-123", "challenge-123");

        assertEquals("nonce-123", nonce);
        verify(valueOps).getAndDelete(eq("mschallenge:session-123:challenge-123"));
    }

    @Test
    void consumeNonce_returnsNullForUnknownChallenge() {
        when(valueOps.getAndDelete("mschallenge:session-123:missing")).thenReturn(null);

        assertNull(store.consumeNonce("session-123", "missing"));
    }
}