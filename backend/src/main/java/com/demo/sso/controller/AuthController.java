package com.demo.sso.controller;

import com.demo.sso.config.AuthRolloutProperties;
import com.demo.sso.controller.dto.AuthApiResponse;
import com.demo.sso.controller.dto.AuthCodeExchangeRequest;
import com.demo.sso.controller.dto.ErrorResponse;
import com.demo.sso.controller.dto.GoogleVerifyRequest;
import com.demo.sso.controller.dto.LogoutResponse;
import com.demo.sso.controller.dto.MicrosoftChallengeResponse;
import com.demo.sso.controller.dto.MicrosoftVerifyRequest;
import com.demo.sso.controller.dto.TokenResponse;
import com.demo.sso.model.AuthFlow;
import com.demo.sso.model.User;
import com.demo.sso.service.AuthCodeStore;
import com.demo.sso.service.GoogleTokenVerifier;
import com.demo.sso.service.JwtTokenService;
import com.demo.sso.service.MicrosoftChallengeStore;
import com.demo.sso.service.MicrosoftTokenVerifier;
import com.demo.sso.service.NormalizedIdentity;
import com.demo.sso.service.ProviderIdentityNormalizer;
import com.demo.sso.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserService userService;
    private final JwtTokenService jwtTokenService;
    private final AuthCodeStore authCodeStore;
    private final ProviderIdentityNormalizer providerIdentityNormalizer;
    private final MicrosoftChallengeStore microsoftChallengeStore;
    private final ObjectProvider<MicrosoftTokenVerifier> microsoftTokenVerifierProvider;
    private final AuthRolloutProperties rolloutProperties;
    private final String servletContextPath;

    private static final String MICROSOFT_CHALLENGE_SESSION_COOKIE = "ms_challenge_session";
    private static final Duration MICROSOFT_CHALLENGE_SESSION_TTL = Duration.ofMinutes(5);

    public AuthController(GoogleTokenVerifier googleTokenVerifier,
                           UserService userService, JwtTokenService jwtTokenService,
                           AuthCodeStore authCodeStore,
                           ProviderIdentityNormalizer providerIdentityNormalizer,
                           MicrosoftChallengeStore microsoftChallengeStore,
                           ObjectProvider<MicrosoftTokenVerifier> microsoftTokenVerifierProvider,
                           AuthRolloutProperties rolloutProperties,
                           @Value("${server.servlet.context-path:}") String servletContextPath) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
        this.authCodeStore = authCodeStore;
        this.providerIdentityNormalizer = providerIdentityNormalizer;
        this.microsoftChallengeStore = microsoftChallengeStore;
        this.microsoftTokenVerifierProvider = microsoftTokenVerifierProvider;
        this.rolloutProperties = rolloutProperties;
        this.servletContextPath = servletContextPath == null ? "" : servletContextPath;
    }

    @PostMapping("/google/verify")
    public ResponseEntity<AuthApiResponse> verifyGoogleToken(@RequestBody GoogleVerifyRequest request) {
        if (!rolloutProperties.getGoogle().isClientSideEnabled()) {
            return ResponseEntity.status(503)
                .body(new ErrorResponse("Google client-side login is disabled"));
        }

        String credential = request == null ? null : request.credential();
        if (credential == null || credential.isBlank()) {
            return badRequest("Missing credential");
        }

        try {
            GoogleIdToken.Payload payload = googleTokenVerifier.verify(credential);

            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Email not verified by Google"));
            }

            NormalizedIdentity identity = providerIdentityNormalizer.normalizeGoogleClaims(
                payload.getSubject(),
                payload.getEmail(),
                (String) payload.get("name"),
                (String) payload.get("picture"),
                AuthFlow.CLIENT_SIDE);

            User user = userService.findOrCreateUser(identity);

            String jwt = jwtTokenService.generateToken(user);

            return ResponseEntity.ok(new TokenResponse(jwt));
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            logger.warn("Google token verification failed: invalid credential");
            return badRequest("Invalid Google credential");
        }
    }

    @PostMapping("/microsoft/challenge")
    public ResponseEntity<AuthApiResponse> issueMicrosoftChallenge(HttpServletRequest request, HttpServletResponse response) {
        if (!rolloutProperties.getMicrosoft().isClientSideEnabled()) {
            return microsoftClientSideDisabledResponse();
        }

        String sessionId = currentChallengeSessionId(request).orElseGet(() -> createChallengeSessionCookie(response));
        MicrosoftChallengeStore.MicrosoftChallenge challenge = microsoftChallengeStore.issueChallenge(sessionId);

        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(new MicrosoftChallengeResponse(challenge.challengeId(), challenge.nonce()));
    }

    @PostMapping("/microsoft/verify")
    public ResponseEntity<AuthApiResponse> verifyMicrosoftToken(
            @RequestBody MicrosoftVerifyRequest request,
            HttpServletRequest httpRequest) {
        if (!rolloutProperties.getMicrosoft().isClientSideEnabled()) {
            return microsoftClientSideDisabledResponse();
        }

        if (request == null || (request.credential() == null || request.credential().isBlank()) || (request.challengeId() == null || request.challengeId().isBlank())) {
            return badRequest("Missing credential or challengeId");
        }

        String sessionId = currentChallengeSessionId(httpRequest).orElse(null);
        if (sessionId == null) {
            return badRequest("Invalid or expired Microsoft challenge");
        }

        Optional<String> expectedNonce = microsoftChallengeStore.consumeNonce(sessionId, request.challengeId());
        if (expectedNonce.isEmpty()) {
            return badRequest("Invalid or expired Microsoft challenge");
        }

        MicrosoftTokenVerifier microsoftTokenVerifier = microsoftTokenVerifierProvider.getIfAvailable();
        if (microsoftTokenVerifier == null) {
            return ResponseEntity.status(503)
                .body(new ErrorResponse("Microsoft client-side login is disabled"));
        }

        try {
            NormalizedIdentity identity = microsoftTokenVerifier.verifyIdToken(
                request.credential(),
                expectedNonce.get(),
                AuthFlow.CLIENT_SIDE);
            User user = userService.findOrCreateUser(identity);
            return ResponseEntity.ok(new TokenResponse(jwtTokenService.generateToken(user)));
        } catch (IllegalArgumentException e) {
            logger.warn("Microsoft token verification failed: invalid credential");
            return badRequest("Invalid Microsoft credential");
        }
    }

    @PostMapping("/exchange")
    public ResponseEntity<AuthApiResponse> exchangeCode(@RequestBody AuthCodeExchangeRequest request) {
        String code = request == null ? null : request.code();
        if (code == null || code.isBlank()) {
            return badRequest("Missing code");
        }

        try {
            String jwt = authCodeStore.exchangeCode(code);
            return ResponseEntity.ok(new TokenResponse(jwt));
        } catch (IllegalArgumentException e) {
            logger.debug("Auth code exchange failed: invalid or expired code");
            return badRequest("Invalid or expired code");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthApiResponse> logout() {
        return ResponseEntity.ok(new LogoutResponse("Logged out successfully"));
    }

    private Optional<String> currentChallengeSessionId(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }

        for (Cookie cookie : request.getCookies()) {
            if (MICROSOFT_CHALLENGE_SESSION_COOKIE.equals(cookie.getName())
                    && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return Optional.of(cookie.getValue());
            }
        }

        return Optional.empty();
    }

    private String createChallengeSessionCookie(HttpServletResponse response) {
        String sessionId = UUID.randomUUID().toString();
        ResponseCookie cookie = ResponseCookie.from(MICROSOFT_CHALLENGE_SESSION_COOKIE, sessionId)
            .httpOnly(true)
            .sameSite("Lax")
            .path(cookiePath())
            .maxAge(MICROSOFT_CHALLENGE_SESSION_TTL)
            .build();
        response.addHeader("Set-Cookie", cookie.toString());
        return sessionId;
    }

    private String cookiePath() {
        if (servletContextPath.isBlank() || "/".equals(servletContextPath)) {
            return "/auth/microsoft";
        }
        return servletContextPath + "/auth/microsoft";
    }

    private ResponseEntity<AuthApiResponse> microsoftClientSideDisabledResponse() {
        return ResponseEntity.status(503)
            .body(new ErrorResponse("Microsoft client-side login is disabled"));
    }

    private ResponseEntity<AuthApiResponse> badRequest(String message) {
        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }
}
