package com.demo.sso.service.challenge;

import com.demo.sso.config.properties.AuthRolloutProperties;
import com.demo.sso.exception.InvalidAuthCodeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisAuthCodeStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private RedisAuthCodeStore store;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        store = new RedisAuthCodeStore(redisTemplate);
    }

    @Test
    void createAuthCode_returnsNonEmptyCode() {
        String code = store.createAuthCode("some.jwt.token");
        assertNotNull(code);
        assertFalse(code.isBlank());
    }

    @Test
    void createAuthCode_alwaysUsesV2Namespace() {
        store.createAuthCode("my.jwt.token");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

        verify(valueOps).set(keyCaptor.capture(), valueCaptor.capture(), ttlCaptor.capture());

        assertTrue(keyCaptor.getValue().startsWith("authcode:v2:"));
        assertEquals("my.jwt.token", valueCaptor.getValue());
        assertEquals(Duration.ofSeconds(30), ttlCaptor.getValue());
    }

    @Test
    void exchangeCode_findsV2Key() {
        when(valueOps.getAndDelete("authcode:v2:test-code")).thenReturn("stored.jwt");

        String jwt = store.exchangeCode("test-code");

        assertEquals("stored.jwt", jwt);
        verify(valueOps).getAndDelete("authcode:v2:test-code");
    }

    @Test
    void exchangeCode_fallsBackToLegacyKeyForInflightCodes() {
        when(valueOps.getAndDelete("authcode:v2:legacy-code")).thenReturn(null);
        when(valueOps.getAndDelete("authcode:legacy-code")).thenReturn("legacy.jwt");

        String jwt = store.exchangeCode("legacy-code");

        assertEquals("legacy.jwt", jwt);
    }

    @Test
    void exchangeCode_throwsForUnknownCode() {
        when(valueOps.getAndDelete("authcode:v2:nonexistent")).thenReturn(null);
        when(valueOps.getAndDelete("authcode:nonexistent")).thenReturn(null);

        assertThrows(InvalidAuthCodeException.class, () -> store.exchangeCode("nonexistent"));
    }

    @Test
    void createAuthCode_generatesUniqueCodes() {
        String code1 = store.createAuthCode("jwt1");
        String code2 = store.createAuthCode("jwt2");
        assertNotEquals(code1, code2);
    }
}
