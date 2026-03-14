package com.demo.sso.service;

/**
 * Manages Microsoft OIDC challenge sessions for the client-side auth flow.
 *
 * <p>A challenge session is tied to a browser session cookie ({@code ms_challenge_session})
 * and has a short TTL (5 minutes by default). Each session can hold one active challenge
 * at a time.
 */
public interface MicrosoftChallengeStore {

    /**
     * Issues a new nonce-based challenge for the given session.
     *
     * <p>Replaces any existing challenge for the session. The caller is responsible
     * for persisting the returned {@code challengeId} and presenting it back during
     * verification.
     *
     * @param sessionId the opaque browser session identifier from the challenge cookie
     * @return a new {@link MicrosoftChallenge} containing the challenge ID and nonce to embed in the MSAL request
     */
    MicrosoftChallenge issueChallenge(String sessionId);

    /**
     * Consumes and returns the nonce associated with a challenge, then invalidates it (single-use).
     *
     * <p>Returns {@code null} if the session does not exist, has expired, or the
     * {@code challengeId} does not match the expected active challenge. The caller must treat
     * a {@code null} return as an invalid or replayed challenge.
     *
     * @param sessionId   the opaque browser session identifier
     * @param challengeId the challenge ID returned by {@link #issueChallenge(String)}
     * @return the nonce to verify against the Microsoft ID token, or {@code null} if invalid/expired
     */
    String consumeNonce(String sessionId, String challengeId);

    /** Immutable value object representing a newly issued challenge. */
    record MicrosoftChallenge(String challengeId, String nonce) {}
}