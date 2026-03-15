package com.demo.sso.service.auth;

import com.demo.sso.model.AuthFlow;
import com.demo.sso.model.User;
import com.demo.sso.service.user.UserService;
import com.demo.sso.service.challenge.AuthCodeStore;
import com.demo.sso.service.model.NormalizedIdentity;
import com.demo.sso.service.token.GoogleTokenVerifier.VerifiedGoogleIdentity;
import com.demo.sso.service.token.JwtTokenService;
import com.demo.sso.service.token.MicrosoftIdTokenClaims;
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
    private final ProviderIdentityNormalizer providerIdentityNormalizer;

    public AuthCompletionService(UserService userService,
                                  JwtTokenService jwtTokenService,
                                  AuthCodeStore authCodeStore,
                                  ProviderIdentityNormalizer providerIdentityNormalizer) {
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
        this.authCodeStore = authCodeStore;
        this.providerIdentityNormalizer = providerIdentityNormalizer;
    }

    /** For client-side flows: syncs user state and returns a JWT directly. */
    public String completeAuthentication(NormalizedIdentity identity) {
        User user = userService.syncUser(identity);
        String jwt = jwtTokenService.generateToken(user);
        logger.info("Authentication completed: provider={}, email={}, flow={}",
            identity.provider(), identity.email(), identity.loginFlow());
        return jwt;
    }

    /** For server-side flows: syncs user state, mints a JWT, and returns an auth code for exchange. */
    public String completeAuthenticationWithCode(NormalizedIdentity identity) {
        String jwt = completeAuthentication(identity);
        return authCodeStore.storeJwt(jwt);
    }

    /** Normalizes Google claims, syncs user state, and returns a JWT. */
    public String completeGoogleAuthentication(VerifiedGoogleIdentity google, AuthFlow flow) {
        NormalizedIdentity identity = providerIdentityNormalizer.normalizeGoogleClaims(google, flow);
        return completeAuthentication(identity);
    }

    /** Normalizes Microsoft claims, syncs user state, and returns a JWT. */
    public String completeMicrosoftAuthentication(MicrosoftIdTokenClaims claims, AuthFlow flow) {
        NormalizedIdentity identity = providerIdentityNormalizer.normalizeMicrosoftClaims(claims, flow);
        return completeAuthentication(identity);
    }
}
