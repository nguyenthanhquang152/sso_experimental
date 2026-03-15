package com.demo.sso.service.auth;

import com.demo.sso.exception.InvalidIdentityException;
import com.demo.sso.model.AuthFlow;
import com.demo.sso.service.token.GoogleTokenVerifier.VerifiedGoogleIdentity;
import com.demo.sso.service.token.MicrosoftIdTokenClaims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @Mock private AuthCompletionService authCompletionService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private OAuth2AuthenticationToken authentication;
    @Mock private OAuth2User oAuth2User;

    private OAuth2SuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2SuccessHandler(authCompletionService, "http://localhost:8000");
    }

    private void setupVerifiedOAuth2User(String sub, String email, String name, String picture) {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn("google");
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
    void onAuthenticationSuccess_completesGoogleAuthAndRedirects() throws IOException {
        setupVerifiedOAuth2User("google-123", "user@example.com", "Test User", "http://pic.url");

        when(authCompletionService.completeGoogleAuthenticationWithCode(
            any(VerifiedGoogleIdentity.class), eq(AuthFlow.SERVER_SIDE)))
            .thenReturn("auth-code-123");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(authCompletionService).completeGoogleAuthenticationWithCode(
            any(VerifiedGoogleIdentity.class), eq(AuthFlow.SERVER_SIDE));
        verify(response).sendRedirect("http://localhost:8000/?code=auth-code-123");
    }

    @Test
    void onAuthenticationSuccess_savesUserAsServerSide() throws IOException {
        setupVerifiedOAuth2User("g-id", "test@test.com", "Name", null);

        when(authCompletionService.completeGoogleAuthenticationWithCode(
            any(VerifiedGoogleIdentity.class), eq(AuthFlow.SERVER_SIDE)))
            .thenReturn("code");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(authCompletionService).completeGoogleAuthenticationWithCode(
            any(VerifiedGoogleIdentity.class), eq(AuthFlow.SERVER_SIDE));
    }

    @Test
    void onAuthenticationSuccess_redirectsToConfiguredFrontendUrl() throws IOException {
        OAuth2SuccessHandler customHandler = new OAuth2SuccessHandler(
            authCompletionService, "https://myapp.com");

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn("google");
        when(oAuth2User.getAttribute("email_verified")).thenReturn(true);
        when(oAuth2User.getAttribute("sub")).thenReturn("g");
        when(oAuth2User.getAttribute("email")).thenReturn("e@e.com");

        when(authCompletionService.completeGoogleAuthenticationWithCode(
            any(VerifiedGoogleIdentity.class), eq(AuthFlow.SERVER_SIDE)))
            .thenReturn("code-xyz");

        customHandler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("https://myapp.com/?code=code-xyz");
    }

    @Test
    void onAuthenticationSuccess_rejectsUnverifiedEmail() throws IOException {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn("google");
        when(oAuth2User.getAttribute("email_verified")).thenReturn(false);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("http://localhost:8000/?error=email_not_verified");
        verifyNoInteractions(authCompletionService);
    }

    @Test
    void onAuthenticationSuccess_rejectsNullEmailVerified() throws IOException {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn("google");
        when(oAuth2User.getAttribute("email_verified")).thenReturn(null);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("http://localhost:8000/?error=email_not_verified");
        verifyNoInteractions(authCompletionService);
    }

    @Test
    void onAuthenticationSuccess_rejectsMissingSub() throws IOException {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn("google");
        when(oAuth2User.getAttribute("email_verified")).thenReturn(true);
        when(oAuth2User.getAttribute("sub")).thenReturn(null);
        when(oAuth2User.getAttribute("email")).thenReturn("test@test.com");

        when(authCompletionService.completeGoogleAuthenticationWithCode(
            any(VerifiedGoogleIdentity.class), eq(AuthFlow.SERVER_SIDE)))
            .thenThrow(new InvalidIdentityException("Google identity is missing required claims"));

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("http://localhost:8000/?error=missing_attributes");
    }

    @Test
    void onAuthenticationSuccess_rejectsMissingEmail() throws IOException {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn("google");
        when(oAuth2User.getAttribute("email_verified")).thenReturn(true);
        when(oAuth2User.getAttribute("sub")).thenReturn("google-123");
        when(oAuth2User.getAttribute("email")).thenReturn(null);

        when(authCompletionService.completeGoogleAuthenticationWithCode(
            any(VerifiedGoogleIdentity.class), eq(AuthFlow.SERVER_SIDE)))
            .thenThrow(new InvalidIdentityException("Google identity is missing required claims"));

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("http://localhost:8000/?error=missing_attributes");
    }

    @Test
    void onAuthenticationSuccess_delegatesAuthCompletionToService() throws IOException {
        setupVerifiedOAuth2User("google-123", "user@example.com", "Test User", "http://pic.url");

        when(authCompletionService.completeGoogleAuthenticationWithCode(
            any(VerifiedGoogleIdentity.class), eq(AuthFlow.SERVER_SIDE)))
            .thenReturn("auth-code-123");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(authCompletionService).completeGoogleAuthenticationWithCode(
            any(VerifiedGoogleIdentity.class), eq(AuthFlow.SERVER_SIDE));
    }

    @Test
    void onAuthenticationSuccess_normalizesMicrosoftServerSideIdentityAndRedirects() throws IOException {
        OAuth2AuthenticationToken microsoftAuthentication = new OAuth2AuthenticationToken(
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

        when(authCompletionService.completeMicrosoftAuthenticationWithCode(
            any(MicrosoftIdTokenClaims.class), eq(AuthFlow.SERVER_SIDE)))
            .thenReturn("ms-auth-code");

        handler.onAuthenticationSuccess(request, response, microsoftAuthentication);

        verify(authCompletionService).completeMicrosoftAuthenticationWithCode(
            any(MicrosoftIdTokenClaims.class), eq(AuthFlow.SERVER_SIDE));
        verify(response).sendRedirect("http://localhost:8000/?code=ms-auth-code");
    }
}
