package com.demo.sso.controller;

import com.demo.sso.controller.dto.UserResponse;
import com.demo.sso.service.model.AuthenticatedUserIdentity;
import com.demo.sso.service.auth.UserService;
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

    /**
     * Returns the current user's profile.
     *
     * <p>The {@code else} branch is a <b>legacy identity fallback</b> that constructs
     * a legacy identity from the raw principal name when the JWT filter has not yet
     * set an {@link AuthenticatedUserIdentity} as the principal. This can happen
     * during the migration if an older JWT (without contract-version claims) is
     * presented.
     *
     * <p><b>Removal:</b> once {@code IdentityContractMode.V2_ONLY} is deployed and
     * all outstanding legacy JWTs have expired, this branch becomes unreachable and
     * can be replaced with a direct cast (or an error response).
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        AuthenticatedUserIdentity identity;
        if (authentication.getPrincipal() instanceof AuthenticatedUserIdentity current) {
            identity = current;
        } else {
            // Legacy fallback: remove when all JWTs minted with V2 format have expired
            logger.warn("Legacy identity fallback: principal type={}", authentication.getPrincipal().getClass().getSimpleName());
            identity = AuthenticatedUserIdentity.legacy(authentication.getName());
        }

        return userService.findCurrentUser(identity)
                .map(user -> ResponseEntity.ok(UserResponse.from(user)))
                .orElse(ResponseEntity.notFound().build());
    }
}
