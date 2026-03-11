package com.demo.sso.service;

import com.demo.sso.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtTokenService jwtTokenService;
    private final AuthCodeStore authCodeStore;
    private final String frontendUrl;

    public OAuth2SuccessHandler(UserService userService, JwtTokenService jwtTokenService,
                                 AuthCodeStore authCodeStore,
                                 @Value("${app.frontend-url}") String frontendUrl) {
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
        this.authCodeStore = authCodeStore;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String googleId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        User user = userService.findOrCreateUser(googleId, email, name, picture, "SERVER_SIDE");

        String jwt = jwtTokenService.generateToken(user.getEmail(), user.getGoogleId());
        String code = authCodeStore.storeJwt(jwt);

        String encodedCode = URLEncoder.encode(code, StandardCharsets.UTF_8);
        response.sendRedirect(frontendUrl + "/?code=" + encodedCode);
    }
}
