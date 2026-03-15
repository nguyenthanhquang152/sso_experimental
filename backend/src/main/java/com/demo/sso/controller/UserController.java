package com.demo.sso.controller;

import com.demo.sso.controller.dto.UserResponse;
import com.demo.sso.service.model.AuthenticatedUserIdentity;
import com.demo.sso.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        AuthenticatedUserIdentity identity;
        if (authentication.getPrincipal() instanceof AuthenticatedUserIdentity current) {
            identity = current;
        } else {
            logger.warn("Legacy identity fallback: principal type={}", authentication.getPrincipal().getClass().getSimpleName());
            identity = AuthenticatedUserIdentity.legacy(authentication.getName(), null);
        }

        return userService.findCurrentUser(identity)
                .map(user -> ResponseEntity.ok(UserResponse.from(user)))
                .orElse(ResponseEntity.notFound().build());
    }
}
