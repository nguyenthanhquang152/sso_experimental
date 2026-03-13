package com.demo.sso.service;

import com.demo.sso.model.AuthFlow;
import com.demo.sso.model.AuthProvider;
import com.demo.sso.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @Mock private UserService userService;
    @Mock private JwtTokenService jwtTokenService;
    @Mock private AuthCodeStore authCodeStore;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private Authentication authentication;
    @Mock private OAuth2User oAuth2User;

    private OAuth2SuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2SuccessHandler(
            userService, jwtTokenService, authCodeStore, "http://localhost:8000");
    }

    private void setupVerifiedOAuth2User(String sub, String email, String name, String picture) {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email_verified")).thenReturn(true);
        when(oAuth2User.getAttribute("sub")).thenReturn(sub);
        when(oAuth2User.getAttribute("email")).thenReturn(email);
        if (name != null) {
            when(oAuth2User.getAttribute("name")).thenReturn(name);
        }
        if (picture != null) {
            when(oAuth2User.getAttribute("picture")).thenReturn(picture);
        }
    }

    @Test
    void onAuthenticationSuccess_extractsAttributesDualWritesGoogleIdentityAndRedirects() throws IOException {
        setupVerifiedOAuth2User("google-123", "user@example.com", "Test User", "http://pic.url");

        User user = new User();
        user.setEmail("user@example.com");
        user.setGoogleId("google-123");
        ArgumentCaptor<NormalizedIdentity> identityCaptor = ArgumentCaptor.forClass(NormalizedIdentity.class);
        when(userService.findOrCreateUser(any(NormalizedIdentity.class))).thenReturn(user);

        when(jwtTokenService.generateToken(user)).thenReturn("test.jwt.token");
        when(authCodeStore.storeJwt("test.jwt.token")).thenReturn("auth-code-123");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(userService).findOrCreateUser(identityCaptor.capture());
        NormalizedIdentity identity = identityCaptor.getValue();
        assertEquals(AuthProvider.GOOGLE, identity.provider());
        assertEquals(AuthFlow.SERVER_SIDE, identity.loginFlow());
        assertEquals("google-123", identity.providerUserId());
        assertEquals("user@example.com", identity.email());
        assertEquals("Test User", identity.name());
        assertEquals("http://pic.url", identity.pictureUrl());
        verify(response).sendRedirect("http://localhost:8000/?code=auth-code-123");
    }

    @Test
    void onAuthenticationSuccess_savesUserAsServerSide() throws IOException {
        setupVerifiedOAuth2User("g-id", "test@test.com", "Name", null);

        User user = new User();
        user.setEmail("test@test.com");
        user.setGoogleId("g-id");
        when(userService.findOrCreateUser(any(NormalizedIdentity.class)))
            .thenReturn(user);
        when(jwtTokenService.generateToken(user)).thenReturn("jwt");
        when(authCodeStore.storeJwt(anyString())).thenReturn("code");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(userService).findOrCreateUser(
            NormalizedIdentity.google("g-id", "test@test.com", "Name", null, AuthFlow.SERVER_SIDE)
        );
    }

    @Test
    void onAuthenticationSuccess_redirectsToConfiguredFrontendUrl() throws IOException {
        OAuth2SuccessHandler customHandler = new OAuth2SuccessHandler(
            userService, jwtTokenService, authCodeStore, "https://myapp.com");

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email_verified")).thenReturn(true);
        when(oAuth2User.getAttribute("sub")).thenReturn("g");
        when(oAuth2User.getAttribute("email")).thenReturn("e@e.com");

        User user = new User();
        user.setEmail("e@e.com");
        user.setGoogleId("g");
        when(userService.findOrCreateUser(any(NormalizedIdentity.class))).thenReturn(user);
        when(jwtTokenService.generateToken(user)).thenReturn("jwt");
        when(authCodeStore.storeJwt("jwt")).thenReturn("code-xyz");

        customHandler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("https://myapp.com/?code=code-xyz");
    }

    @Test
    void onAuthenticationSuccess_rejectsUnverifiedEmail() throws IOException {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email_verified")).thenReturn(false);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("http://localhost:8000/?error=email_not_verified");
        verifyNoInteractions(userService);
    }

    @Test
    void onAuthenticationSuccess_rejectsNullEmailVerified() throws IOException {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email_verified")).thenReturn(null);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("http://localhost:8000/?error=email_not_verified");
        verifyNoInteractions(userService);
    }

    @Test
    void onAuthenticationSuccess_rejectsMissingSub() throws IOException {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email_verified")).thenReturn(true);
        when(oAuth2User.getAttribute("sub")).thenReturn(null);
        when(oAuth2User.getAttribute("email")).thenReturn("test@test.com");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("http://localhost:8000/?error=missing_attributes");
        verifyNoInteractions(userService);
    }

    @Test
    void onAuthenticationSuccess_rejectsMissingEmail() throws IOException {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email_verified")).thenReturn(true);
        when(oAuth2User.getAttribute("sub")).thenReturn("google-123");
        when(oAuth2User.getAttribute("email")).thenReturn(null);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("http://localhost:8000/?error=missing_attributes");
        verifyNoInteractions(userService);
    }

    @Test
    void onAuthenticationSuccess_delegatesJwtMintingToRolloutAwareUserMethod() throws IOException {
        setupVerifiedOAuth2User("google-123", "user@example.com", "Test User", "http://pic.url");

        User user = new User();
        user.setId(42L);
        user.setEmail("user@example.com");
        user.setGoogleId("google-123");
        user.setProvider(AuthProvider.GOOGLE);
        user.setProviderUserId("google-123");
        when(userService.findOrCreateUser(any(NormalizedIdentity.class))).thenReturn(user);
        when(jwtTokenService.generateToken(user)).thenReturn("test.jwt.token");
        when(authCodeStore.storeJwt("test.jwt.token")).thenReturn("auth-code-123");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(jwtTokenService).generateToken(user);
        verify(jwtTokenService, never()).generateToken(anyString(), anyString());
    }
}
