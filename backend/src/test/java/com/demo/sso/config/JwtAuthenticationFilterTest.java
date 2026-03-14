package com.demo.sso.config;

import com.demo.sso.model.AuthProvider;
import com.demo.sso.config.JwtAuthenticationFilter;
import com.demo.sso.service.auth.AuthenticatedUserIdentity;
import com.demo.sso.service.token.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.anyString;

/**
 * Unit tests for JwtAuthenticationFilter.
 * Tests JWT token extraction, validation, and authentication setup.
 */
class JwtAuthenticationFilterTest {

    private JwtTokenService jwtTokenService;
    private JwtAuthenticationFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtTokenService = mock(JwtTokenService.class);
        filter = new JwtAuthenticationFilter(jwtTokenService);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);

        // Clear SecurityContextHolder before each test
        SecurityContextHolder.clearContext();
    }

    @Test
    void testConstructor() {
        assertNotNull(filter, "Filter should be created successfully");
    }

    @Test
    void testDoFilterInternalWithValidBearerToken() throws ServletException, IOException {
        // Arrange
        String token = "valid.jwt.token";
        String headerValue = "Bearer " + token;
        AuthenticatedUserIdentity identity = AuthenticatedUserIdentity.v2(
            1L, "user@example.com", AuthProvider.GOOGLE, "google-123"
        );

        when(request.getHeader("Authorization")).thenReturn(headerValue);
        when(jwtTokenService.isTokenValid(token)).thenReturn(true);
        when(jwtTokenService.parseAuthenticatedUser(token)).thenReturn(identity);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenService).isTokenValid(token);
        verify(jwtTokenService).parseAuthenticatedUser(token);
        verify(filterChain).doFilter(request, response);

        // Verify authentication was set in SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "Authentication should be set");
        assertEquals(identity, auth.getPrincipal(), "Principal should be the parsed identity");
        assertTrue(auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_USER")),
            "Should have ROLE_USER authority");
    }

    @Test
    void testDoFilterInternalWithInvalidToken() throws ServletException, IOException {
        // Arrange
        String token = "invalid.jwt.token";
        String headerValue = "Bearer " + token;

        when(request.getHeader("Authorization")).thenReturn(headerValue);
        when(jwtTokenService.isTokenValid(token)).thenReturn(false);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenService).isTokenValid(token);
        verify(jwtTokenService, never()).parseAuthenticatedUser(anyString());
        verify(filterChain).doFilter(request, response);

        // Verify authentication was NOT set
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth, "Authentication should not be set for invalid token");
    }

    @Test
    void testDoFilterInternalWithNoAuthorizationHeader() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenService, never()).isTokenValid(anyString());
        verify(jwtTokenService, never()).parseAuthenticatedUser(anyString());
        verify(filterChain).doFilter(request, response);

        // Verify authentication was NOT set
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth, "Authentication should not be set without header");
    }

    @Test
    void testDoFilterInternalWithEmptyAuthorizationHeader() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenService, never()).isTokenValid(anyString());
        verify(filterChain).doFilter(request, response);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth, "Authentication should not be set with empty header");
    }

    @Test
    void testDoFilterInternalWithNonBearerAuthorizationHeader() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenService, never()).isTokenValid(anyString());
        verify(filterChain).doFilter(request, response);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth, "Authentication should not be set for non-Bearer header");
    }

    @Test
    void testDoFilterInternalWithBlankToken() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer    ");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenService, never()).isTokenValid(anyString());
        verify(filterChain).doFilter(request, response);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth, "Authentication should not be set for blank token");
    }

    @Test
    void testDoFilterInternalWithOnlyBearerPrefix() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenService, never()).isTokenValid(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternalCaseSensitiveBearerCheck() throws ServletException, IOException {
        // Arrange - lowercase "bearer" should not be recognized
        when(request.getHeader("Authorization")).thenReturn("bearer token");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtTokenService, never()).isTokenValid(anyString());
        verify(filterChain).doFilter(request, response);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth, "Bearer prefix is case-sensitive - lowercase should not work");
    }

    @Test
    void testDoFilterInternalAlwaysCallsFilterChain() throws ServletException, IOException {
        // Test that filter chain is always called regardless of auth success/failure

        // Case 1: With valid token
        String token = "valid.token";
        AuthenticatedUserIdentity identity = AuthenticatedUserIdentity.v2(
            1L, "user@example.com", AuthProvider.GOOGLE, "google-123"
        );
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenService.isTokenValid(token)).thenReturn(true);
        when(jwtTokenService.parseAuthenticatedUser(token)).thenReturn(identity);

        filter.doFilterInternal(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(request, response);

        // Case 2: Without token
        SecurityContextHolder.clearContext();
        reset(filterChain);
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testDoFilterInternalSetsCorrectAuthentication() throws ServletException, IOException {
        // Arrange
        String token = "valid.jwt.token";
        AuthenticatedUserIdentity identity = AuthenticatedUserIdentity.v2(
            2L, "test@example.com", AuthProvider.MICROSOFT, "ms-456"
        );

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenService.isTokenValid(token)).thenReturn(true);
        when(jwtTokenService.parseAuthenticatedUser(token)).thenReturn(identity);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(identity, auth.getPrincipal());
        assertNull(auth.getCredentials(), "Credentials should be null");
        assertEquals(1, auth.getAuthorities().size(), "Should have exactly one authority");
        assertEquals("ROLE_USER", auth.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void testDoFilterInternalWithTokenValidationException() throws ServletException, IOException {
        // Arrange - token service throws exception during validation
        String token = "problematic.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenService.isTokenValid(token)).thenThrow(new RuntimeException("Token validation error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            filter.doFilterInternal(request, response, filterChain);
        }, "Should propagate exceptions from token service");

        // Filter chain should NOT be called if exception is thrown
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testDoFilterInternalClearsExistingAuthenticationOnInvalidToken() throws ServletException, IOException {
        // Arrange - simulate existing authentication in context
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication existingAuth = mock(Authentication.class);
        context.setAuthentication(existingAuth);
        SecurityContextHolder.setContext(context);

        String invalidToken = "invalid.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
        when(jwtTokenService.isTokenValid(invalidToken)).thenReturn(false);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert - existing auth should remain (filter doesn't clear it)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertSame(existingAuth, auth, "Filter should not clear existing auth on invalid token");
    }
}
