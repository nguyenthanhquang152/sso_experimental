package com.demo.sso.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.demo.sso.config.properties.AuthRolloutProperties;
import com.demo.sso.controller.dto.AuthCodeExchangeRequest;
import com.demo.sso.controller.dto.GoogleVerifyRequest;
import com.demo.sso.dto.LogoutResponse;
import com.demo.sso.dto.TokenResponse;
import com.demo.sso.dto.AuthApiResponse;
import com.demo.sso.dto.ErrorResponse;
import com.demo.sso.exception.InvalidAuthCodeException;
import com.demo.sso.model.AuthFlow;
import com.demo.sso.service.auth.AuthCompletionService;
import com.demo.sso.service.auth.ProviderIdentityNormalizer;
import com.demo.sso.service.challenge.AuthCodeStore;
import com.demo.sso.service.challenge.MicrosoftChallengeStore;
import com.demo.sso.service.model.NormalizedIdentity;
import com.demo.sso.service.token.GoogleTokenVerifier;
import com.demo.sso.service.model.VerifiedGoogleIdentity;
import com.demo.sso.service.token.MicrosoftTokenVerifier;
import com.demo.sso.exception.InvalidTokenException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    @Mock
    private AuthCompletionService authCompletionService;

    @Mock
    private ProviderIdentityNormalizer providerIdentityNormalizer;

    @Mock
    private AuthCodeStore authCodeStore;

    @Mock
    private MicrosoftChallengeStore microsoftChallengeStore;

    @Mock
    private MicrosoftTokenVerifier microsoftTokenVerifier;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        AuthRolloutProperties rollout = new AuthRolloutProperties();
        controller = new AuthController(
                googleTokenVerifier,
                authCompletionService,
                providerIdentityNormalizer,
                authCodeStore,
                microsoftChallengeStore,
                microsoftTokenVerifier,
                rollout,
                "");
    }

    // --- logout ---

    @Test
    void logoutReturnsSuccessMessage() {
        ResponseEntity<AuthApiResponse> response = controller.logout();

        assertEquals(200, response.getStatusCode().value());
        assertInstanceOf(LogoutResponse.class, response.getBody());
        assertEquals("Logged out successfully", ((LogoutResponse) response.getBody()).message());
    }

    // --- exchange ---

    @Test
    void exchangeReturnsTokenForValidCode() {
        when(authCodeStore.exchangeCode("valid-code")).thenReturn("jwt-token-123");

        ResponseEntity<AuthApiResponse> response = controller.exchangeCode(
                new AuthCodeExchangeRequest("valid-code"));

        assertEquals(200, response.getStatusCode().value());
        assertInstanceOf(TokenResponse.class, response.getBody());
        assertEquals("jwt-token-123", ((TokenResponse) response.getBody()).token());
    }

    @Test
    void exchangeReturnsBadRequestForNullCode() {
        ResponseEntity<AuthApiResponse> response = controller.exchangeCode(
                new AuthCodeExchangeRequest(null));

        assertEquals(400, response.getStatusCode().value());
        assertInstanceOf(ErrorResponse.class, response.getBody());
        assertEquals("Missing code", ((ErrorResponse) response.getBody()).message());
    }

    @Test
    void exchangeReturnsBadRequestForBlankCode() {
        ResponseEntity<AuthApiResponse> response = controller.exchangeCode(
                new AuthCodeExchangeRequest("   "));

        assertEquals(400, response.getStatusCode().value());
        assertInstanceOf(ErrorResponse.class, response.getBody());
    }

    @Test
    void exchangeReturnsBadRequestForInvalidOrExpiredCode() {
        when(authCodeStore.exchangeCode("expired-code"))
                .thenThrow(new InvalidAuthCodeException("expired"));

        ResponseEntity<AuthApiResponse> response = controller.exchangeCode(
                new AuthCodeExchangeRequest("expired-code"));

        assertEquals(400, response.getStatusCode().value());
        assertInstanceOf(ErrorResponse.class, response.getBody());
        assertEquals("Invalid or expired code", ((ErrorResponse) response.getBody()).message());
    }

    // --- google/verify ---

    @Test
    void googleVerifyReturnsTokenForValidCredential() throws GeneralSecurityException, IOException {
        VerifiedGoogleIdentity identity = new VerifiedGoogleIdentity(
                "google-sub-123", "alice@gmail.com", true, "Alice", "https://photo.url");
        when(googleTokenVerifier.verifyIdToken("valid-id-token")).thenReturn(identity);

        NormalizedIdentity normalizedIdentity = NormalizedIdentity.google(
                "google-sub-123", "alice@gmail.com", "Alice", "https://photo.url", AuthFlow.CLIENT_SIDE);
        when(providerIdentityNormalizer.normalizeGoogleClaims(eq(identity), eq(AuthFlow.CLIENT_SIDE)))
                .thenReturn(normalizedIdentity);
        when(authCompletionService.completeAuthentication(normalizedIdentity))
                .thenReturn("jwt-google-token");

        ResponseEntity<AuthApiResponse> response = controller.verifyGoogleToken(
                new GoogleVerifyRequest("valid-id-token"));

        assertEquals(200, response.getStatusCode().value());
        assertInstanceOf(TokenResponse.class, response.getBody());
        assertEquals("jwt-google-token", ((TokenResponse) response.getBody()).token());
    }

    @Test
    void googleVerifyReturnsBadRequestForNullCredential() {
        ResponseEntity<AuthApiResponse> response = controller.verifyGoogleToken(
                new GoogleVerifyRequest(null));

        assertEquals(400, response.getStatusCode().value());
        assertInstanceOf(ErrorResponse.class, response.getBody());
        assertEquals("Missing credential", ((ErrorResponse) response.getBody()).message());
    }

    @Test
    void googleVerifyReturnsBadRequestForBlankCredential() {
        ResponseEntity<AuthApiResponse> response = controller.verifyGoogleToken(
                new GoogleVerifyRequest("  "));

        assertEquals(400, response.getStatusCode().value());
        assertInstanceOf(ErrorResponse.class, response.getBody());
    }

    @Test
    void googleVerifyReturnsBadRequestForInvalidToken() throws GeneralSecurityException, IOException {
        when(googleTokenVerifier.verifyIdToken("bad-token"))
                .thenThrow(new InvalidTokenException("Invalid Google ID token"));

        ResponseEntity<AuthApiResponse> response = controller.verifyGoogleToken(
                new GoogleVerifyRequest("bad-token"));

        assertEquals(400, response.getStatusCode().value());
        assertInstanceOf(ErrorResponse.class, response.getBody());
        assertEquals("Invalid Google credential", ((ErrorResponse) response.getBody()).message());
    }

    @Test
    void googleVerifyReturnsBadRequestForUnverifiedEmail() throws GeneralSecurityException, IOException {
        VerifiedGoogleIdentity identity = new VerifiedGoogleIdentity(
                "google-sub-456", "unverified@gmail.com", false, "Bob", null);
        when(googleTokenVerifier.verifyIdToken("unverified-token")).thenReturn(identity);
        when(providerIdentityNormalizer.normalizeGoogleClaims(eq(identity), eq(AuthFlow.CLIENT_SIDE)))
                .thenThrow(new com.demo.sso.exception.InvalidIdentityException("Google email not verified"));

        ResponseEntity<AuthApiResponse> response = controller.verifyGoogleToken(
                new GoogleVerifyRequest("unverified-token"));

        assertEquals(400, response.getStatusCode().value());
        assertInstanceOf(ErrorResponse.class, response.getBody());
        assertEquals("Invalid Google credential", ((ErrorResponse) response.getBody()).message());
    }

    @Test
    void googleVerifyReturns503WhenClientSideDisabled() {
        AuthRolloutProperties rollout = new AuthRolloutProperties();
        rollout.getGoogle().setClientSideEnabled(false);

        AuthController disabledController = new AuthController(
                googleTokenVerifier, authCompletionService, providerIdentityNormalizer, authCodeStore,
                microsoftChallengeStore, microsoftTokenVerifier, rollout, "");

        ResponseEntity<AuthApiResponse> response = disabledController.verifyGoogleToken(
                new GoogleVerifyRequest("any-token"));

        assertEquals(503, response.getStatusCode().value());
        assertInstanceOf(ErrorResponse.class, response.getBody());
    }

    @Test
    void googleVerifyReturnsBadRequestOnGeneralSecurityException() throws GeneralSecurityException, IOException {
        when(googleTokenVerifier.verifyIdToken("crypto-fail"))
                .thenThrow(new GeneralSecurityException("crypto error"));

        ResponseEntity<AuthApiResponse> response = controller.verifyGoogleToken(
                new GoogleVerifyRequest("crypto-fail"));

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Invalid Google credential", ((ErrorResponse) response.getBody()).message());
    }
}
