package com.demo.sso.controller;

import com.demo.sso.model.User;
import com.demo.sso.service.AuthCodeStore;
import com.demo.sso.service.GoogleTokenVerifier;
import com.demo.sso.service.JwtTokenService;
import com.demo.sso.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserService userService;
    private final JwtTokenService jwtTokenService;
    private final AuthCodeStore authCodeStore;

    public AuthController(GoogleTokenVerifier googleTokenVerifier,
                           UserService userService, JwtTokenService jwtTokenService,
                           AuthCodeStore authCodeStore) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
        this.authCodeStore = authCodeStore;
    }

    @PostMapping("/google/verify")
    public ResponseEntity<?> verifyGoogleToken(@RequestBody Map<String, String> body) {
        String credential = body.get("credential");
        if (credential == null || credential.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing credential"));
        }

        try {
            GoogleIdToken.Payload payload = googleTokenVerifier.verify(credential);

            String googleId = payload.getSubject();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");

            User user = userService.findOrCreateUser(googleId, email, name, picture, "CLIENT_SIDE");

            String jwt = jwtTokenService.generateToken(user.getEmail(), user.getGoogleId());

            return ResponseEntity.ok(Map.of("token", jwt));
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            logger.warn("Google token verification failed", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid Google credential"));
        }
    }

    @PostMapping("/exchange")
    public ResponseEntity<?> exchangeCode(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing code"));
        }

        String jwt = authCodeStore.exchangeCode(code);
        if (jwt == null) {
            logger.debug("Auth code exchange failed for code: {}...",
                    code.length() > 8 ? code.substring(0, 8) : code);
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired code"));
        }

        return ResponseEntity.ok(Map.of("token", jwt));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
