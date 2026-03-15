package com.demo.sso.service.token;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

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
     * @param idTokenString the raw ID token from the client
     * @return verified identity claims
     * @throws IllegalArgumentException if the token is invalid
     * @throws GeneralSecurityException on crypto errors
     * @throws IOException on network errors
     */
    public VerifiedGoogleIdentity verify(String idTokenString)
            throws GeneralSecurityException, IOException {
        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            throw new IllegalArgumentException("Invalid Google ID token");
        }
        GoogleIdToken.Payload payload = idToken.getPayload();
        return new VerifiedGoogleIdentity(
                payload.getSubject(),
                payload.getEmail(),
                Boolean.TRUE.equals(payload.getEmailVerified()),
                (String) payload.get("name"),
                (String) payload.get("picture"));
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
    }
}
