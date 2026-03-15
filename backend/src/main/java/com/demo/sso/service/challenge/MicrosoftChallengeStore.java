package com.demo.sso.service.challenge;

import java.util.Optional;

/**
 * Manages Microsoft OIDC challenge sessions for the client-side auth flow.
 *
 * <p>A challenge session is tied to a browser session cookie ({@code ms_challenge_session})
 * and has a short TTL (5 minutes by default). Each session can hold one active challenge
 * at a time.
 */
public interface MicrosoftChallengeStore {

    /** Replaces any existing challenge for the session. */
    MicrosoftChallenge issueChallenge(String sessionId);

    /**
     * Consumes the nonce (single-use). Returns empty if session is expired
     * or challengeId doesn't match.
     */
    Optional<String> consumeNonce(String sessionId, String challengeId);

    record MicrosoftChallenge(String challengeId, String nonce) {}
}