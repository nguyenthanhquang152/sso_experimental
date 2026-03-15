package com.demo.sso.service.auth;

import com.demo.sso.model.AuthFlow;
import com.demo.sso.model.AuthProvider;
import com.demo.sso.model.User;
import com.demo.sso.service.user.UserService;
import com.demo.sso.service.challenge.AuthCodeStore;
import com.demo.sso.service.token.GoogleTokenVerifier.VerifiedGoogleIdentity;
import com.demo.sso.service.token.JwtTokenService;
import com.demo.sso.service.token.MicrosoftIdTokenClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthCompletionServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private AuthCodeStore authCodeStore;

    private AuthCompletionService service;

    private NormalizedIdentity googleIdentity;
    private NormalizedIdentity microsoftIdentity;
    private User testUser;

    @BeforeEach
    void setUp() {
        service = new AuthCompletionService(userService, jwtTokenService, authCodeStore, new ProviderIdentityNormalizer());

        googleIdentity = NormalizedIdentity.google(
                "google-sub-123", "user@gmail.com", "Test User",
                "https://example.com/pic.jpg", AuthFlow.CLIENT_SIDE);

        microsoftIdentity = NormalizedIdentity.microsoft(
                "ms-oid-456", "user@outlook.com", "MS User",
                "https://example.com/ms-pic.jpg", AuthFlow.SERVER_SIDE);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("user@gmail.com");
        testUser.setName("Test User");
        testUser.setProvider(AuthProvider.GOOGLE);
        testUser.setProviderUserId("google-sub-123");
        testUser.setGoogleId("google-sub-123");
    }

    // --- completeAuthentication tests ---

    @Test
    void completeAuthentication_findsOrCreatesUserAndReturnsJwt() {
        when(userService.findOrCreateUser(googleIdentity)).thenReturn(testUser);
        when(jwtTokenService.generateToken(testUser)).thenReturn("jwt-token-abc");

        String jwt = service.completeAuthentication(googleIdentity);

        assertEquals("jwt-token-abc", jwt);
        verify(userService).findOrCreateUser(googleIdentity);
        verify(jwtTokenService).generateToken(testUser);
        verifyNoInteractions(authCodeStore);
    }

    @Test
    void completeAuthentication_worksForMicrosoftProvider() {
        User msUser = new User();
        msUser.setId(2L);
        msUser.setEmail("user@outlook.com");
        msUser.setProvider(AuthProvider.MICROSOFT);
        msUser.setProviderUserId("ms-oid-456");

        when(userService.findOrCreateUser(microsoftIdentity)).thenReturn(msUser);
        when(jwtTokenService.generateToken(msUser)).thenReturn("jwt-ms-token");

        String jwt = service.completeAuthentication(microsoftIdentity);

        assertEquals("jwt-ms-token", jwt);
        verify(userService).findOrCreateUser(microsoftIdentity);
        verify(jwtTokenService).generateToken(msUser);
    }

    @Test
    void completeAuthentication_propagatesUserServiceException() {
        when(userService.findOrCreateUser(googleIdentity))
                .thenThrow(new IllegalStateException("Concurrent user creation failed"));

        assertThrows(IllegalStateException.class,
                () -> service.completeAuthentication(googleIdentity));
        verify(jwtTokenService, never()).generateToken(any());
    }

    @Test
    void completeAuthentication_propagatesTokenGenerationException() {
        when(userService.findOrCreateUser(googleIdentity)).thenReturn(testUser);
        when(jwtTokenService.generateToken(testUser))
                .thenThrow(new IllegalArgumentException("V2 JWT minting requires id"));

        assertThrows(IllegalArgumentException.class,
                () -> service.completeAuthentication(googleIdentity));
    }

    // --- completeAuthenticationWithCode tests ---

    @Test
    void completeAuthenticationWithCode_returnsAuthCode() {
        when(userService.findOrCreateUser(microsoftIdentity)).thenReturn(testUser);
        when(jwtTokenService.generateToken(testUser)).thenReturn("jwt-for-code");
        when(authCodeStore.storeJwt("jwt-for-code")).thenReturn("auth-code-xyz");

        String code = service.completeAuthenticationWithCode(microsoftIdentity);

        assertEquals("auth-code-xyz", code);
        verify(authCodeStore).storeJwt("jwt-for-code");
    }

    @Test
    void completeAuthenticationWithCode_chainsCompleteAuthentication() {
        when(userService.findOrCreateUser(googleIdentity)).thenReturn(testUser);
        when(jwtTokenService.generateToken(testUser)).thenReturn("chained-jwt");
        when(authCodeStore.storeJwt("chained-jwt")).thenReturn("code-123");

        String code = service.completeAuthenticationWithCode(googleIdentity);

        assertEquals("code-123", code);
        verify(userService).findOrCreateUser(googleIdentity);
        verify(jwtTokenService).generateToken(testUser);
        verify(authCodeStore).storeJwt("chained-jwt");
    }

    @Test
    void completeAuthenticationWithCode_propagatesAuthCodeStoreException() {
        when(userService.findOrCreateUser(googleIdentity)).thenReturn(testUser);
        when(jwtTokenService.generateToken(testUser)).thenReturn("jwt-ok");
        when(authCodeStore.storeJwt("jwt-ok"))
                .thenThrow(new RuntimeException("Redis unavailable"));

        assertThrows(RuntimeException.class,
                () -> service.completeAuthenticationWithCode(googleIdentity));
    }

    // --- completeGoogleAuthentication tests ---

    @Test
    void completeGoogleAuthentication_normalizesAndCompletesAuthentication() {
        when(userService.findOrCreateUser(any(NormalizedIdentity.class))).thenReturn(testUser);
        when(jwtTokenService.generateToken(testUser)).thenReturn("google-jwt");

        VerifiedGoogleIdentity google = new VerifiedGoogleIdentity(
            "google-sub-123", "user@gmail.com", true, "Test User", "https://example.com/pic.jpg");
        String jwt = service.completeGoogleAuthentication(google, AuthFlow.CLIENT_SIDE);

        assertEquals("google-jwt", jwt);
        verify(userService).findOrCreateUser(any(NormalizedIdentity.class));
    }

    // --- completeMicrosoftAuthentication tests ---

    @Test
    void completeMicrosoftAuthentication_normalizesAndCompletesAuthentication() {
        User msUser = new User();
        msUser.setId(2L);
        msUser.setEmail("user@outlook.com");
        msUser.setProvider(AuthProvider.MICROSOFT);
        msUser.setProviderUserId("ms-oid-456");

        when(userService.findOrCreateUser(any(NormalizedIdentity.class))).thenReturn(msUser);
        when(jwtTokenService.generateToken(msUser)).thenReturn("ms-jwt");

        MicrosoftIdTokenClaims claims = new MicrosoftIdTokenClaims(
            "https://login.microsoftonline.com/tid/v2.0",
            "ms-sub",
            "tid",
            "user@outlook.com",
            null, null,
            "MS User",
            null, null, "2.0", "nonce",
            List.of("client-id"));

        String jwt = service.completeMicrosoftAuthentication(claims, AuthFlow.CLIENT_SIDE);

        assertEquals("ms-jwt", jwt);
        verify(userService).findOrCreateUser(any(NormalizedIdentity.class));
    }
}
