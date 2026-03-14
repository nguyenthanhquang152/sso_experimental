package com.demo.sso.service;

import com.demo.sso.service.OAuth2SuccessHandler;
import com.demo.sso.service.auth.AuthCompletionService;
import com.demo.sso.service.auth.NormalizedIdentity;
import com.demo.sso.service.auth.ProviderIdentityNormalizer;
import com.demo.sso.model.AuthFlow;
import com.demo.sso.model.AuthProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.any;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @Mock private AuthCompletionService authCompletionService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private Authentication authentication;
    @Mock private OAuth2User oAuth2User;

    private OAuth2SuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2SuccessHandler(
            authCompletionService, new ProviderIdentityNormalizer(), "http://localhost:8000");
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

        ArgumentCaptor<NormalizedIdentity> identityCaptor = ArgumentCaptor.forClass(NormalizedIdentity.class);
        when(authCompletionService.completeAuthenticationWithCode(any(NormalizedIdentity.class)))
            .thenReturn("auth-code-123");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(authCompletionService).completeAuthenticationWithCode(identityCaptor.capture());
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

        when(authCompletionService.completeAuthenticationWithCode(any(NormalizedIdentity.class)))
            .thenReturn("code");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(authCompletionService).completeAuthenticationWithCode(
            NormalizedIdentity.google("g-id", "test@test.com", "Name", null, AuthFlow.SERVER_SIDE)
        );
    }

    @Test
    void onAuthenticationSuccess_redirectsToConfiguredFrontendUrl() throws IOException {
        OAuth2SuccessHandler customHandler = new OAuth2SuccessHandler(
            authCompletionService, new ProviderIdentityNormalizer(), "https://myapp.com");

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email_verified")).thenReturn(true);
        when(oAuth2User.getAttribute("sub")).thenReturn("g");
        when(oAuth2User.getAttribute("email")).thenReturn("e@e.com");

        when(authCompletionService.completeAuthenticationWithCode(any(NormalizedIdentity.class)))
            .thenReturn("code-xyz");

        customHandler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("https://myapp.com/?code=code-xyz");
    }

    @Test
    void onAuthenticationSuccess_rejectsUnverifiedEmail() throws IOException {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email_verified")).thenReturn(false);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("http://localhost:8000/?error=email_not_verified");
        verifyNoInteractions(authCompletionService);
    }

    @Test
    void onAuthenticationSuccess_rejectsNullEmailVerified() throws IOException {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email_verified")).thenReturn(null);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("http://localhost:8000/?error=email_not_verified");
        verifyNoInteractions(authCompletionService);
    }

    @Test
    void onAuthenticationSuccess_rejectsMissingSub() throws IOException {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email_verified")).thenReturn(true);
        when(oAuth2User.getAttribute("sub")).thenReturn(null);
        when(oAuth2User.getAttribute("email")).thenReturn("test@test.com");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("http://localhost:8000/?error=missing_attributes");
        verifyNoInteractions(authCompletionService);
    }

    @Test
    void onAuthenticationSuccess_rejectsMissingEmail() throws IOException {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email_verified")).thenReturn(true);
        when(oAuth2User.getAttribute("sub")).thenReturn("google-123");
        when(oAuth2User.getAttribute("email")).thenReturn(null);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("http://localhost:8000/?error=missing_attributes");
        verifyNoInteractions(authCompletionService);
    }

    @Test
    void onAuthenticationSuccess_delegatesAuthCompletionToService() throws IOException {
        setupVerifiedOAuth2User("google-123", "user@example.com", "Test User", "http://pic.url");

        when(authCompletionService.completeAuthenticationWithCode(any(NormalizedIdentity.class)))
            .thenReturn("auth-code-123");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(authCompletionService).completeAuthenticationWithCode(any(NormalizedIdentity.class));
    }

    @Test
    void onAuthenticationSuccess_normalizesMicrosoftServerSideIdentityAndRedirects() throws IOException {
        Authentication microsoftAuthentication = new OAuth2AuthenticationToken(
            new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of(
                    "iss", "https://login.microsoftonline.com/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/v2.0",
                    "tid", "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
                    "sub", "microsoft-subject",
                    "email", "employee@example.com",
                    "name", "Microsoft Employee"
                ),
                "sub"
            ),
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            "microsoft"
        );

        ArgumentCaptor<NormalizedIdentity> identityCaptor = ArgumentCaptor.forClass(NormalizedIdentity.class);
        when(authCompletionService.completeAuthenticationWithCode(any(NormalizedIdentity.class)))
            .thenReturn("ms-auth-code");

        handler.onAuthenticationSuccess(request, response, microsoftAuthentication);

        verify(authCompletionService).completeAuthenticationWithCode(identityCaptor.capture());
        NormalizedIdentity identity = identityCaptor.getValue();
        assertEquals(AuthProvider.MICROSOFT, identity.provider());
        assertEquals(AuthFlow.SERVER_SIDE, identity.loginFlow());
        assertEquals(
            "https://login.microsoftonline.com/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/v2.0|microsoft-subject",
            identity.providerUserId());
        assertEquals("employee@example.com", identity.email());
        assertEquals("Microsoft Employee", identity.name());
        verify(response).sendRedirect("http://localhost:8000/?code=ms-auth-code");
    }
}
