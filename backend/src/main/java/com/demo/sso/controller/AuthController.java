package com.demo.sso.controller;

import com.demo.sso.config.JwtConfig;
import com.demo.sso.model.User;
import com.demo.sso.service.GoogleTokenVerifier;
import com.demo.sso.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserService userService;
    private final JwtConfig jwtConfig;

    public AuthController(GoogleTokenVerifier googleTokenVerifier,
                           UserService userService, JwtConfig jwtConfig) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.userService = userService;
        this.jwtConfig = jwtConfig;
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

            String jwt = jwtConfig.generateToken(user.getEmail(), user.getGoogleId());

            return ResponseEntity.ok(Map.of("token", jwt));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid token: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
