package com.demo.sso.service;

import com.demo.sso.model.AuthFlow;
import com.demo.sso.model.User;
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
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    private final UserService userService;
    private final JwtTokenService jwtTokenService;
    private final AuthCodeStore authCodeStore;
    private final ProviderIdentityNormalizer providerIdentityNormalizer;
    private final String frontendUrl;

    public OAuth2SuccessHandler(UserService userService, JwtTokenService jwtTokenService,
                                 AuthCodeStore authCodeStore,
                                 ProviderIdentityNormalizer providerIdentityNormalizer,
                                 @Value("${app.frontend-url}") String frontendUrl) {
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
        this.authCodeStore = authCodeStore;
        this.providerIdentityNormalizer = providerIdentityNormalizer;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Optional<NormalizedIdentity> identity = normalizeIdentity(authentication, oAuth2User, response);
        if (identity.isEmpty()) {
            return;
        }

        User user = userService.findOrCreateUser(identity.get());

        String jwt = jwtTokenService.generateToken(user);
        String code = authCodeStore.storeJwt(jwt);

        String encodedCode = URLEncoder.encode(code, StandardCharsets.UTF_8);
        response.sendRedirect(frontendUrl + "/?code=" + encodedCode);
    }

    private Optional<NormalizedIdentity> normalizeIdentity(
            Authentication authentication,
            OAuth2User oAuth2User,
            HttpServletResponse response) throws IOException {
        String registrationId = authentication instanceof OAuth2AuthenticationToken oauth2AuthenticationToken
            ? oauth2AuthenticationToken.getAuthorizedClientRegistrationId()
            : "google";

        if ("microsoft".equalsIgnoreCase(registrationId)) {
            try {
                return Optional.of(providerIdentityNormalizer.normalizeMicrosoftClaims(
                    oAuth2User.getAttributes(),
                    AuthFlow.SERVER_SIDE));
            } catch (IllegalArgumentException e) {
                logger.warn("Microsoft OAuth2 login rejected: invalid identity claims");
                response.sendRedirect(frontendUrl + "/?error=missing_attributes");
                return Optional.empty();
            }
        }

        Boolean emailVerified = oAuth2User.getAttribute("email_verified");
        if (!Boolean.TRUE.equals(emailVerified)) {
            logger.warn("OAuth2 login rejected: email not verified");
            response.sendRedirect(frontendUrl + "/?error=email_not_verified");
            return Optional.empty();
        }

        try {
            return Optional.of(providerIdentityNormalizer.normalizeGoogleClaims(
                oAuth2User.getAttribute("sub"),
                oAuth2User.getAttribute("email"),
                oAuth2User.getAttribute("name"),
                oAuth2User.getAttribute("picture"),
                AuthFlow.SERVER_SIDE));
        } catch (IllegalArgumentException e) {
            logger.warn("OAuth2 login rejected: missing sub or email attribute");
            response.sendRedirect(frontendUrl + "/?error=missing_attributes");
            return Optional.empty();
        }
    }
}
