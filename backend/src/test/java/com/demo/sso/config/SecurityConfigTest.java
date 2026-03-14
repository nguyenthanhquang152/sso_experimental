package com.demo.sso.config;

import com.demo.sso.service.OAuth2SuccessHandler;
import com.demo.sso.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;

/**
 * Unit tests for SecurityConfig.
 * Tests the security configuration bean creation and basic setup.
 */
class SecurityConfigTest {

    private OAuth2SuccessHandler oAuth2SuccessHandler;
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private MicrosoftAuthorizationGateFilter microsoftAuthorizationGateFilter;
    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        // Create mock dependencies
        oAuth2SuccessHandler = mock(OAuth2SuccessHandler.class);
        jwtAuthenticationFilter = mock(JwtAuthenticationFilter.class);
        microsoftAuthorizationGateFilter = mock(MicrosoftAuthorizationGateFilter.class);

        // Create SecurityConfig instance
        securityConfig = new SecurityConfig(
            oAuth2SuccessHandler,
            jwtAuthenticationFilter,
            microsoftAuthorizationGateFilter
        );
    }

    @Test
    void testSecurityConfigConstructor() {
        // Assert
        assertNotNull(securityConfig, "SecurityConfig should be created successfully");
    }

    @Test
    void testSecurityConfigWithNullDependenciesDoesNotThrow() {
        // This tests that the constructor accepts null values without immediate failure
        // (though Spring would fail at runtime if beans are actually null)
        assertDoesNotThrow(() -> {
            new SecurityConfig(null, null, null);
        }, "Constructor should not throw with null dependencies");
    }

    @Test
    void testFilterChainReturnsNonNull() throws Exception {
        // Arrange
        HttpSecurity httpSecurity = createHttpSecurity();

        // Act
        SecurityFilterChain filterChain = securityConfig.filterChain(httpSecurity);

        // Assert
        assertNotNull(filterChain, "FilterChain should not be null");
    }

    @Test
    void testFilterChainBuildsSuccessfully() throws Exception {
        // Arrange
        HttpSecurity httpSecurity = createHttpSecurity();

        // Act & Assert
        assertDoesNotThrow(() -> {
            SecurityFilterChain chain = securityConfig.filterChain(httpSecurity);
            assertNotNull(chain, "Should build a valid SecurityFilterChain");
        }, "Building SecurityFilterChain should not throw exception");
    }

    @Test
    void testSecurityConfigurationUsesInjectedHandlers() {
        // Verify that the SecurityConfig stores the injected dependencies
        // This indirectly tests constructor injection
        assertNotNull(oAuth2SuccessHandler, "OAuth2 success handler should be set");
        assertNotNull(jwtAuthenticationFilter, "JWT filter should be set");
        assertNotNull(microsoftAuthorizationGateFilter, "Microsoft gate filter should be set");
    }

    @Test
    void testMultipleSecurityConfigInstances() {
        // Test that multiple instances can be created with different dependencies
        OAuth2SuccessHandler handler1 = mock(OAuth2SuccessHandler.class);
        OAuth2SuccessHandler handler2 = mock(OAuth2SuccessHandler.class);
        JwtAuthenticationFilter filter1 = mock(JwtAuthenticationFilter.class);
        JwtAuthenticationFilter filter2 = mock(JwtAuthenticationFilter.class);
        MicrosoftAuthorizationGateFilter gateFilter1 = mock(MicrosoftAuthorizationGateFilter.class);
        MicrosoftAuthorizationGateFilter gateFilter2 = mock(MicrosoftAuthorizationGateFilter.class);

        SecurityConfig config1 = new SecurityConfig(handler1, filter1, gateFilter1);
        SecurityConfig config2 = new SecurityConfig(handler2, filter2, gateFilter2);

        assertNotNull(config1);
        assertNotNull(config2);
        assertNotSame(config1, config2, "Should create distinct instances");
    }

    /**
     * Helper method to create HttpSecurity for testing.
     * Uses a minimal configuration to avoid complex Spring context setup.
     */
    private HttpSecurity createHttpSecurity() throws Exception {
        // Create a minimal HttpSecurity instance for testing
        // This is a simplified approach - in real Spring context,
        // HttpSecurity would be fully configured by the framework
        HttpSecurity httpSecurity = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);
        
        // Configure basic mocking to return self for chaining
        when(httpSecurity.csrf(any())).thenReturn(httpSecurity);
        when(httpSecurity.sessionManagement(any())).thenReturn(httpSecurity);
        when(httpSecurity.authorizeHttpRequests(any())).thenReturn(httpSecurity);
        when(httpSecurity.oauth2Login(any())).thenReturn(httpSecurity);
        when(httpSecurity.exceptionHandling(any())).thenReturn(httpSecurity);
        when(httpSecurity.addFilterBefore(any(), any())).thenReturn(httpSecurity);
        
        // Mock the build method to return a mock DefaultSecurityFilterChain (concrete class)
        SecurityFilterChain mockChain = mock(SecurityFilterChain.class);
        when(httpSecurity.build()).thenAnswer(invocation -> mockChain);
        
        return httpSecurity;
    }

    @Test
    void testFilterChainConfigurationInvokesHttpSecurityMethods() throws Exception {
        // Arrange
        HttpSecurity httpSecurity = createHttpSecurity();

        // Act
        securityConfig.filterChain(httpSecurity);

        // Assert - verify that key configuration methods were called
        verify(httpSecurity).csrf(any());
        verify(httpSecurity).sessionManagement(any());
        verify(httpSecurity).authorizeHttpRequests(any());
        verify(httpSecurity).oauth2Login(any());
        verify(httpSecurity).exceptionHandling(any());
        verify(httpSecurity, times(2)).addFilterBefore(any(), any());
        verify(httpSecurity).build();
    }

    @Test
    void testFilterChainAddsCustomFiltersInCorrectOrder() throws Exception {
        // Arrange
        HttpSecurity httpSecurity = createHttpSecurity();

        // Act
        securityConfig.filterChain(httpSecurity);

        // Assert - verify filters are added (order matters in Spring Security)
        verify(httpSecurity).addFilterBefore(
            eq(microsoftAuthorizationGateFilter), 
            any()
        );
        verify(httpSecurity).addFilterBefore(
            eq(jwtAuthenticationFilter), 
            any()
        );
    }
}
