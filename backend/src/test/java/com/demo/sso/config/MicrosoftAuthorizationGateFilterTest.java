package com.demo.sso.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MicrosoftAuthorizationGateFilter.
 * Tests Microsoft OAuth endpoint gating based on rollout configuration.
 */
class MicrosoftAuthorizationGateFilterTest {

    private AuthRolloutProperties rolloutProperties;
    private MicrosoftAuthorizationGateFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;
    private PrintWriter writer;
    private String frontendUrl = "http://localhost:3000";

    @BeforeEach
    void setUp() throws IOException {
        rolloutProperties = new AuthRolloutProperties();
        filter = new MicrosoftAuthorizationGateFilter(rolloutProperties, frontendUrl);
        
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        writer = mock(PrintWriter.class);
        
        when(response.getWriter()).thenReturn(writer);
        
        // Clear SecurityContext before each test
        SecurityContextHolder.clearContext();
    }

    @Test
    void testConstructor() {
        assertNotNull(filter, "Filter should be created successfully");
    }

    @Test
    void testAuthorizationEndpointWhenMicrosoftDisabled() throws ServletException, IOException {
        // Arrange
        rolloutProperties.getMicrosoft().setServerSideEnabled(false);
        when(request.getRequestURI()).thenReturn("/oauth2/authorization/microsoft");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        verify(response).setContentType("application/json");
        verify(writer).write("{\"error\":\"Microsoft server-side login is disabled\"}");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testAuthorizationEndpointWhenMicrosoftEnabled() throws ServletException, IOException {
        // Arrange
        rolloutProperties.getMicrosoft().setServerSideEnabled(true);
        when(request.getRequestURI()).thenReturn("/oauth2/authorization/microsoft");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response, never()).setStatus(anyInt());
        verify(response, never()).setContentType(anyString());
        verify(writer, never()).write(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testCallbackEndpointWhenMicrosoftDisabled() throws ServletException, IOException {
        // Arrange
        rolloutProperties.getMicrosoft().setServerSideEnabled(false);
        when(request.getRequestURI()).thenReturn("/login/oauth2/code/microsoft");
        
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        
        // Set some authentication in SecurityContext to verify it gets cleared
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(mock(org.springframework.security.core.Authentication.class));
        SecurityContextHolder.setContext(context);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(session).invalidate();
        verify(response).sendRedirect(frontendUrl + "/?error=provider_disabled&provider=microsoft&flow=server_side");
        verify(filterChain, never()).doFilter(request, response);
        
        // Verify SecurityContext was cleared
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "SecurityContext should be cleared");
    }

    @Test
    void testCallbackEndpointWhenMicrosoftDisabledWithoutSession() throws ServletException, IOException {
        // Arrange
        rolloutProperties.getMicrosoft().setServerSideEnabled(false);
        when(request.getRequestURI()).thenReturn("/login/oauth2/code/microsoft");
        when(request.getSession(false)).thenReturn(null); // No session

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).sendRedirect(frontendUrl + "/?error=provider_disabled&provider=microsoft&flow=server_side");
        verify(filterChain, never()).doFilter(request, response);
        
        // Should not throw NPE when session is null
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testCallbackEndpointWhenMicrosoftEnabled() throws ServletException, IOException {
        // Arrange
        rolloutProperties.getMicrosoft().setServerSideEnabled(true);
        when(request.getRequestURI()).thenReturn("/login/oauth2/code/microsoft");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response, never()).sendRedirect(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testOtherPathsPassThrough() throws ServletException, IOException {
        // Arrange - test various non-Microsoft paths
        rolloutProperties.getMicrosoft().setServerSideEnabled(false); // Even when disabled
        
        String[] testPaths = {
            "/",
            "/api/users",
            "/oauth2/authorization/google",
            "/login/oauth2/code/google",
            "/auth/providers",
            "/health"
        };

        for (String path : testPaths) {
            reset(request, response, filterChain);
            when(request.getRequestURI()).thenReturn(path);

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(response, never()).setStatus(anyInt());
            verify(response, never()).sendRedirect(anyString());
        }
    }

    @Test
    void testPathMatchingIsCaseSensitive() throws ServletException, IOException {
        // Arrange - paths with different casing should pass through
        rolloutProperties.getMicrosoft().setServerSideEnabled(false);
        
        String[] nonMatchingPaths = {
            "/oauth2/authorization/Microsoft", // Capital M
            "/oauth2/AUTHORIZATION/microsoft", // Capital AUTHORIZATION
            "/login/oauth2/code/MICROSOFT"     // Capital MICROSOFT
        };

        for (String path : nonMatchingPaths) {
            reset(request, response, filterChain);
            when(request.getRequestURI()).thenReturn(path);

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(response, never()).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
    }

    @Test
    void testExactPathMatchRequired() throws ServletException, IOException {
        // Arrange - paths must end exactly with the pattern
        rolloutProperties.getMicrosoft().setServerSideEnabled(false);
        
        when(request.getRequestURI()).thenReturn("/oauth2/authorization/microsoft/extra");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert - should pass through since it doesn't end with the exact path
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }

    @Test
    void testFrontendUrlUsedInRedirect() throws ServletException, IOException {
        // Arrange
        String customFrontendUrl = "https://app.example.com";
        MicrosoftAuthorizationGateFilter customFilter = 
            new MicrosoftAuthorizationGateFilter(rolloutProperties, customFrontendUrl);
        
        rolloutProperties.getMicrosoft().setServerSideEnabled(false);
        when(request.getRequestURI()).thenReturn("/login/oauth2/code/microsoft");
        when(request.getSession(false)).thenReturn(null);

        // Act
        customFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).sendRedirect(
            customFrontendUrl + "/?error=provider_disabled&provider=microsoft&flow=server_side");
    }

    @Test
    void testClientSideEnabledDoesNotAffectFilter() throws ServletException, IOException {
        // Arrange - clientSideEnabled should not affect server-side gating
        rolloutProperties.getMicrosoft().setServerSideEnabled(false);
        rolloutProperties.getMicrosoft().setClientSideEnabled(true); // This should not matter
        
        when(request.getRequestURI()).thenReturn("/oauth2/authorization/microsoft");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert - should still block because serverSideEnabled is false
        verify(response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testBothEndpointsBlockedWhenDisabled() throws ServletException, IOException {
        // Arrange
        rolloutProperties.getMicrosoft().setServerSideEnabled(false);

        // Test authorization endpoint
        when(request.getRequestURI()).thenReturn("/oauth2/authorization/microsoft");
        filter.doFilterInternal(request, response, filterChain);
        verify(response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        verify(filterChain, never()).doFilter(request, response);

        // Reset mocks
        reset(request, response, filterChain);
        when(request.getSession(false)).thenReturn(null);
        
        // Test callback endpoint
        when(request.getRequestURI()).thenReturn("/login/oauth2/code/microsoft");
        filter.doFilterInternal(request, response, filterChain);
        verify(response).sendRedirect(anyString());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testBothEndpointsAllowedWhenEnabled() throws ServletException, IOException {
        // Arrange
        rolloutProperties.getMicrosoft().setServerSideEnabled(true);

        // Test authorization endpoint
        when(request.getRequestURI()).thenReturn("/oauth2/authorization/microsoft");
        filter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);

        // Reset mocks
        reset(request, response, filterChain);
        
        // Test callback endpoint
        when(request.getRequestURI()).thenReturn("/login/oauth2/code/microsoft");
        filter.doFilterInternal(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(request, response);
    }
}
