package com.demo.sso.service.auth;

import com.demo.sso.model.AuthFlow;
import com.demo.sso.service.token.GoogleTokenVerifier.VerifiedGoogleIdentity;
import com.demo.sso.service.token.MicrosoftIdTokenClaims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    private final AuthCompletionService authCompletionService;
    private final String frontendUrl;

    public OAuth2SuccessHandler(AuthCompletionService authCompletionService,
                                 @Value("${app.frontend-url}") String frontendUrl) {
        this.authCompletionService = authCompletionService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String code;
        try {
            code = completeProviderAuthentication(authentication, oAuth2User);
        } catch (OAuth2IdentityException e) {
            logger.warn("OAuth2 login rejected: {}", e.errorCode());
            response.sendRedirect(frontendUrl + "/?error=" + e.errorCode());
            return;
        }

        String encodedCode = URLEncoder.encode(code, StandardCharsets.UTF_8);
        response.sendRedirect(frontendUrl + "/?code=" + encodedCode);
    }

    private String completeProviderAuthentication(
            Authentication authentication,
            OAuth2User oAuth2User) {
        String registrationId = authentication instanceof OAuth2AuthenticationToken oauth2AuthenticationToken
            ? oauth2AuthenticationToken.getAuthorizedClientRegistrationId()
            : "google";

        if ("microsoft".equalsIgnoreCase(registrationId)) {
            try {
                MicrosoftIdTokenClaims claims = MicrosoftIdTokenClaims.fromMap(oAuth2User.getAttributes());
                return authCompletionService.completeMicrosoftAuthenticationWithCode(claims, AuthFlow.SERVER_SIDE);
            } catch (IllegalArgumentException e) {
                throw new OAuth2IdentityException("missing_attributes", "invalid identity claims");
            }
        }

        Boolean emailVerified = oAuth2User.getAttribute("email_verified");
        if (!Boolean.TRUE.equals(emailVerified)) {
            throw new OAuth2IdentityException("email_not_verified", "email not verified");
        }

        try {
            VerifiedGoogleIdentity google = VerifiedGoogleIdentity.fromOAuth2User(oAuth2User);
            return authCompletionService.completeGoogleAuthenticationWithCode(google, AuthFlow.SERVER_SIDE);
        } catch (IllegalArgumentException e) {
            throw new OAuth2IdentityException("missing_attributes", "missing sub or email attribute");
        }
    }

    /** Thrown when OAuth2 identity claims fail validation. */
    static final class OAuth2IdentityException extends RuntimeException {
        private final String errorCode;

        OAuth2IdentityException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        String errorCode() {
            return errorCode;
        }
    }
}
