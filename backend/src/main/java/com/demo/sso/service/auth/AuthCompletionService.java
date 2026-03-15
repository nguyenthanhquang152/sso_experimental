package com.demo.sso.service.auth;

import com.demo.sso.model.User;
import com.demo.sso.service.challenge.AuthCodeStore;
import com.demo.sso.service.model.NormalizedIdentity;
import com.demo.sso.service.token.JwtTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Single application-service boundary for the authentication completion sequence:
 * user-lookup → JWT-mint → (optional auth-code-store).
 */
@Service
public class AuthCompletionService {

    private static final Logger logger = LoggerFactory.getLogger(AuthCompletionService.class);

    private final UserService userService;
    private final JwtTokenService jwtTokenService;
    private final AuthCodeStore authCodeStore;

    public AuthCompletionService(UserService userService,
                                  JwtTokenService jwtTokenService,
                                  AuthCodeStore authCodeStore) {
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
        this.authCodeStore = authCodeStore;
    }

    /** For client-side flows: syncs user state and returns a JWT directly. */
    public String completeAuthentication(NormalizedIdentity identity) {
        User user = userService.syncUser(identity);
        String jwt = jwtTokenService.generateToken(user);
        logger.info("Authentication completed: provider={}, flow={}",
            identity.provider(), identity.loginFlow());
        return jwt;
    }

    /** For server-side flows: syncs user state, mints a JWT, and returns an auth code for exchange. */
    public String completeAuthenticationWithCode(NormalizedIdentity identity) {
        String jwt = completeAuthentication(identity);
        return authCodeStore.createAuthCode(jwt);
    }
}
