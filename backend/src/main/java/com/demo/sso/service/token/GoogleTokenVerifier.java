package com.demo.sso.service.token;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.demo.sso.exception.InvalidTokenException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Verifies Google ID tokens and returns a provider-neutral {@link VerifiedGoogleIdentity}.
 * Encapsulates all Google SDK interaction so callers never depend on the Google API library.
 */
@Service
public class GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    @Autowired
    public GoogleTokenVerifier(@Value("${app.google-client-id}") String clientId) {
        this(new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build());
    }

    /** Package-private constructor for injecting a mock verifier in tests. */
    GoogleTokenVerifier(GoogleIdTokenVerifier verifier) {
        this.verifier = verifier;
    }

    /**
     * Verifies a Google ID token string and extracts identity claims.
     *
     * <p>Unlike {@link MicrosoftTokenVerifier#verifyIdToken}, this method delegates to
     * Google's SDK which declares checked exceptions for crypto and network failures.
     *
     * @param idTokenString the raw ID token from the client
     * @return verified identity claims
     * @throws InvalidTokenException if the token is null after verification (invalid or expired)
     * @throws GeneralSecurityException if Google's token verification library rejects the token
     * @throws IOException if network error during token verification
     */
    public VerifiedGoogleIdentity verifyIdToken(String idTokenString)
            throws GeneralSecurityException, IOException {
        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            throw new InvalidTokenException("Invalid Google ID token");
        }
        GoogleIdToken.Payload payload = idToken.getPayload();
        return new VerifiedGoogleIdentity(
                payload.getSubject(),
                payload.getEmail(),
                Boolean.TRUE.equals(payload.getEmailVerified()),
                payload.get("name") instanceof String name ? name : null,
                payload.get("picture") instanceof String pic ? pic : null);
    }

    /**
     * Provider-neutral representation of a verified Google identity.
     * {@code name} and {@code pictureUrl} may be null if the user hasn't set them.
     */
    public record VerifiedGoogleIdentity(
            String subject,
            String email,
            boolean emailVerified,
            String name,
            String pictureUrl) {

        /** Creates a VerifiedGoogleIdentity from server-side OAuth2 user attributes. */
        public static VerifiedGoogleIdentity fromOAuth2User(OAuth2User user) {
            return new VerifiedGoogleIdentity(
                    user.getAttribute("sub"),
                    user.getAttribute("email"),
                    Boolean.TRUE.equals(user.getAttribute("email_verified")),
                    user.getAttribute("name"),
                    user.getAttribute("picture"));
        }
    }
}
