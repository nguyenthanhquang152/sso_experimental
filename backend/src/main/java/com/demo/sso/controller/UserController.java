package com.demo.sso.controller;

import com.demo.sso.model.User;
import com.demo.sso.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();

        return userService.findByEmail(email)
                .map(user -> ResponseEntity.ok(Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "name", user.getName() != null ? user.getName() : "",
                        "pictureUrl", user.getPictureUrl() != null ? user.getPictureUrl() : "",
                        "loginMethod", user.getLoginMethod() != null ? user.getLoginMethod() : "",
                        "createdAt", user.getCreatedAt().toString(),
                        "lastLoginAt", user.getLastLoginAt().toString()
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}
