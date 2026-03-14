package com.demo.sso.service;

import com.demo.sso.service.MicrosoftChallengeStore;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import java.util.List;
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

        verify(valueOps, org.mockito.Mockito.times(2)).set(
            keyCaptor.capture(),
            valueCaptor.capture(),
            ttlCaptor.capture()
        );

        List<String> keys = keyCaptor.getAllValues();
        List<String> values = valueCaptor.getAllValues();
        List<Duration> ttls = ttlCaptor.getAllValues();

        assertTrue(keys.contains("mschallenge-active:session-123"));
        assertTrue(keys.stream().anyMatch(key -> key.startsWith("mschallenge:session-123:")));
        assertTrue(values.contains(challenge.nonce()));
        assertTrue(values.contains(challenge.challengeId()));
        assertTrue(ttls.stream().allMatch(Duration.ofMinutes(5)::equals));
    }

    @Test
    void issueChallenge_replacesPreviousChallengeForSameSession() {
        when(valueOps.get("mschallenge-active:session-123")).thenReturn("challenge-1");

        store.issueChallenge("session-123");

        verify(redisTemplate).delete("mschallenge:session-123:challenge-1");
        verify(valueOps).set(eq("mschallenge-active:session-123"), anyString(), eq(Duration.ofMinutes(5)));
    }

    @Test
    void consumeNonce_readsAndDeletesChallenge() {
        when(valueOps.get("mschallenge-active:session-123")).thenReturn("challenge-123");
        when(valueOps.getAndDelete("mschallenge:session-123:challenge-123")).thenReturn("nonce-123");

        Optional<String> nonce = store.consumeNonce("session-123", "challenge-123");

        assertEquals(Optional.of("nonce-123"), nonce);
        verify(valueOps).getAndDelete(eq("mschallenge:session-123:challenge-123"));
        verify(redisTemplate).delete("mschallenge-active:session-123");
    }

    @Test
    void consumeNonce_returnsNullForUnknownChallenge() {
        when(valueOps.get("mschallenge-active:session-123")).thenReturn("missing");
        when(valueOps.getAndDelete("mschallenge:session-123:missing")).thenReturn(null);

        assertEquals(Optional.empty(), store.consumeNonce("session-123", "missing"));
    }

    @Test
    void consumeNonce_rejectsChallengeThatIsNotActiveForSession() {
        when(valueOps.get("mschallenge-active:session-123")).thenReturn("challenge-456");

        assertFalse(store.consumeNonce("session-123", "challenge-123").isPresent());
        verify(valueOps, never()).getAndDelete("mschallenge:session-123:challenge-123");
    }
}