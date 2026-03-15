package com.demo.sso.controller;

import com.demo.sso.controller.dto.UserResponse;
import com.demo.sso.service.AuthenticatedUserIdentity;
import com.demo.sso.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        AuthenticatedUserIdentity identity = authentication.getPrincipal() instanceof AuthenticatedUserIdentity current
            ? current
            : AuthenticatedUserIdentity.legacy(authentication.getName(), null);

        return userService.findCurrentUser(identity)
                .map(user -> ResponseEntity.ok(UserResponse.from(user)))
                .orElse(ResponseEntity.notFound().build());
    }
}

