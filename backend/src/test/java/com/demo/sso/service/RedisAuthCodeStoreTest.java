package com.demo.sso.service;

import com.demo.sso.config.AuthRolloutProperties;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

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

    @Test
    void storeJwt_usesV2NamespaceWhenMintModeIsV2() {
        RedisAuthCodeStore v2Store = new RedisAuthCodeStore(
            redisTemplate,
            rolloutProperties(AuthRolloutProperties.IdentityContractMode.COMPATIBILITY, AuthRolloutProperties.JwtMintMode.V2)
        );

        v2Store.storeJwt("v2.jwt.token");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(keyCaptor.capture(), eq("v2.jwt.token"), eq(Duration.ofSeconds(30)));
        assertTrue(keyCaptor.getValue().startsWith("authcode:v2:"));
    }

    @Test
    void exchangeCode_readsLegacyThenV2NamespaceDuringCompatibilityMode() {
        RedisAuthCodeStore compatibilityStore = new RedisAuthCodeStore(
            redisTemplate,
            rolloutProperties(AuthRolloutProperties.IdentityContractMode.COMPATIBILITY, AuthRolloutProperties.JwtMintMode.LEGACY)
        );
        when(valueOps.getAndDelete("authcode:compat-code")).thenReturn(null);
        when(valueOps.getAndDelete("authcode:v2:compat-code")).thenReturn("stored.v2.jwt");

        String jwt = compatibilityStore.exchangeCode("compat-code");

        assertEquals("stored.v2.jwt", jwt);
        verify(valueOps).getAndDelete("authcode:compat-code");
        verify(valueOps).getAndDelete("authcode:v2:compat-code");
    }

    @Test
    void exchangeCode_ignoresLegacyNamespaceWhenContractIsV2Only() {
        RedisAuthCodeStore v2OnlyStore = new RedisAuthCodeStore(
            redisTemplate,
            rolloutProperties(AuthRolloutProperties.IdentityContractMode.V2_ONLY, AuthRolloutProperties.JwtMintMode.V2)
        );
        when(valueOps.getAndDelete("authcode:v2:compat-code")).thenReturn("stored.v2.jwt");

        String jwt = v2OnlyStore.exchangeCode("compat-code");

        assertEquals("stored.v2.jwt", jwt);
        verify(valueOps, never()).getAndDelete("authcode:compat-code");
        verify(valueOps).getAndDelete("authcode:v2:compat-code");
    }

    private static AuthRolloutProperties rolloutProperties(
            AuthRolloutProperties.IdentityContractMode identityContractMode,
            AuthRolloutProperties.JwtMintMode jwtMintMode) {
        AuthRolloutProperties properties = new AuthRolloutProperties();
        properties.setIdentityContractMode(identityContractMode);
        properties.setJwtMintMode(jwtMintMode);
        return properties;
    }
}
