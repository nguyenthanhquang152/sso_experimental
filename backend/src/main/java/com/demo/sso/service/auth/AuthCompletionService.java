package com.demo.sso.service.auth;

import com.demo.sso.model.User;
import com.demo.sso.service.UserService;
import com.demo.sso.service.challenge.AuthCodeStore;
import com.demo.sso.service.token.JwtTokenService;
import org.springframework.stereotype.Service;

/**
 * Single application-service boundary for the authentication completion sequence:
 * user-lookup → JWT-mint → (optional auth-code-store).
 */
@Service
public class AuthCompletionService {

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

    /** For client-side flows: finds/creates user and returns a JWT directly. */
    public String completeAuthentication(NormalizedIdentity identity) {
        User user = userService.findOrCreateUser(identity);
        return jwtTokenService.generateToken(user);
    }

    /** For server-side flows: finds/creates user, mints a JWT, and returns an auth code for exchange. */
    public String completeAuthenticationWithCode(NormalizedIdentity identity) {
        String jwt = completeAuthentication(identity);
        return authCodeStore.storeJwt(jwt);
    }
}
