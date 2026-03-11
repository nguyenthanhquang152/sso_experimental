package com.demo.sso.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
    void storeJwt_returnsNonEmptyCode() {
        String code = store.storeJwt("some.jwt.token");
        assertNotNull(code);
        assertFalse(code.isBlank());
    }

    @Test
    void storeJwt_setsValueInRedisWithTtl() {
        store.storeJwt("my.jwt.token");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

        verify(valueOps).set(keyCaptor.capture(), valueCaptor.capture(), ttlCaptor.capture());

        assertTrue(keyCaptor.getValue().startsWith("authcode:"));
        assertEquals("my.jwt.token", valueCaptor.getValue());
        assertEquals(Duration.ofSeconds(30), ttlCaptor.getValue());
    }

    @Test
    void exchangeCode_callsGetAndDelete() {
        when(valueOps.getAndDelete("authcode:test-code")).thenReturn("stored.jwt");

        String jwt = store.exchangeCode("test-code");

        assertEquals("stored.jwt", jwt);
        verify(valueOps).getAndDelete("authcode:test-code");
    }

    @Test
    void exchangeCode_returnsNullForUnknownCode() {
        when(valueOps.getAndDelete("authcode:nonexistent")).thenReturn(null);

        assertNull(store.exchangeCode("nonexistent"));
    }

    @Test
    void storeJwt_generatesUniqueCodes() {
        String code1 = store.storeJwt("jwt1");
        String code2 = store.storeJwt("jwt2");
        assertNotEquals(code1, code2);
    }
}
