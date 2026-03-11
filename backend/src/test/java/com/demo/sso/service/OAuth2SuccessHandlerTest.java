package com.demo.sso.service;

import com.demo.sso.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
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

    @Test
    void onAuthenticationSuccess_extractsAttributesAndRedirects() throws IOException {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("sub")).thenReturn("google-123");
        when(oAuth2User.getAttribute("email")).thenReturn("user@example.com");
        when(oAuth2User.getAttribute("name")).thenReturn("Test User");
        when(oAuth2User.getAttribute("picture")).thenReturn("http://pic.url");

        User user = new User();
        user.setEmail("user@example.com");
        user.setGoogleId("google-123");
        when(userService.findOrCreateUser("google-123", "user@example.com",
            "Test User", "http://pic.url", "SERVER_SIDE")).thenReturn(user);

        when(jwtTokenService.generateToken("user@example.com", "google-123"))
            .thenReturn("test.jwt.token");
        when(authCodeStore.storeJwt("test.jwt.token")).thenReturn("auth-code-123");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("http://localhost:8000/?code=auth-code-123");
    }

    @Test
    void onAuthenticationSuccess_savesUserAsServerSide() throws IOException {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("sub")).thenReturn("g-id");
        when(oAuth2User.getAttribute("email")).thenReturn("test@test.com");
        when(oAuth2User.getAttribute("name")).thenReturn("Name");
        when(oAuth2User.getAttribute("picture")).thenReturn(null);

        User user = new User();
        user.setEmail("test@test.com");
        user.setGoogleId("g-id");
        when(userService.findOrCreateUser(anyString(), anyString(), anyString(), any(), eq("SERVER_SIDE")))
            .thenReturn(user);
        when(jwtTokenService.generateToken(anyString(), anyString())).thenReturn("jwt");
        when(authCodeStore.storeJwt(anyString())).thenReturn("code");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(userService).findOrCreateUser("g-id", "test@test.com", "Name", null, "SERVER_SIDE");
    }

    @Test
    void onAuthenticationSuccess_redirectsToConfiguredFrontendUrl() throws IOException {
        OAuth2SuccessHandler customHandler = new OAuth2SuccessHandler(
            userService, jwtTokenService, authCodeStore, "https://myapp.com");

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("sub")).thenReturn("g");
        when(oAuth2User.getAttribute("email")).thenReturn("e@e.com");

        User user = new User();
        user.setEmail("e@e.com");
        user.setGoogleId("g");
        when(userService.findOrCreateUser(any(), any(), any(), any(), any())).thenReturn(user);
        when(jwtTokenService.generateToken(any(), any())).thenReturn("jwt");
        when(authCodeStore.storeJwt("jwt")).thenReturn("code-xyz");

        customHandler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("https://myapp.com/?code=code-xyz");
    }
}
